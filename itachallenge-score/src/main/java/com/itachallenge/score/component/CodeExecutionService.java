package com.itachallenge.score.component;

import com.itachallenge.score.dto.ExecutionResultDto;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

@Component
public class CodeExecutionService {

    /*
    RECEPCIÓN DEL CÓDIGO DEL USUARIO
    El código plantilla -- public class Main{ public static void main(String[] args){ }}"; --
    viene por defecto en application.yml. El usuario se limita a introducir la lógica o el algoritmo
    que se le pide en el enunciado.

    La inyección de parámetros al código del cliente se hace mediante el uso varargs, el método castArgs,
    nos permite llamar al método pasando un array de objetos o pasando los objetos directamente, por ejemplo:

                Object[] args = new Object[]{"hola", 1, 2.0};
                compileAndRunCode(sourceCode, codeResult, args);
                compileAndRunCode(sourceCode, codeResult, "hola", 1, 2.0);

    La salida del compilador es a traves del Sistem.out.println
     */

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    @Value("${code.execution.template}")
    private String codeTemplate;

    public ExecutionResultDto compileAndRunCode(String sourceCode, String codeResult, Object... args) {

        // Integrar el código del usuario en la plantilla
        sourceCode = String.format(codeTemplate, sourceCode);


        //Compilar el código
        CompilationResult compilationResult = compile(sourceCode);
        ExecutionResultDto executionResultDto = compilationResult.getExecutionResultDto();

        //Descomposición y tipado de parámetros de entrada
        String[] argsString = castArgs(args);

        if (executionResultDto.isCompiled()) {
            //Ejecutar el código
            ExecutionResult executionResult = execute(compilationResult, codeResult, argsString);
            if (executionResultDto.isExecution()) {
                //Comparar el resultado
                executionResultDto = compareResults(executionResult.getExecutionResultMsg(), codeResult, executionResultDto);
            }
        }
        return executionResultDto;
    }

    public CompilationResult compile(String sourceCode) {
        ExecutionResultDto executionResultDto = new ExecutionResultDto(false, false, false, "");
        SimpleCompiler compiler;

        try {
            compiler = new SimpleCompiler();
            compiler.cook(sourceCode);
            // Si la compilación es exitosa, actualizar ExecutionResultDto
            executionResultDto.setCompiled(true);
        } catch (CompileException e) {
            // Si la compilación falla, actualizar y devolver ExecutionResultDto
            executionResultDto.setMessage("Compilation failed: " + e.getMessage());
            log.error(e.getMessage());
            return new CompilationResult(executionResultDto, null);
        }

        return new CompilationResult(executionResultDto, compiler);
    }

    public ExecutionResult execute(CompilationResult compilationResult, String codeResult, String[] args) {
        ExecutionResult executionResult = new ExecutionResult(compilationResult.getExecutionResultDto(), "");
        ExecutionResultDto executionResultDto = compilationResult.getExecutionResultDto();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        String executionFailedMessage = "Execution failed: ";
        PrintStream old = System.out;
        System.setOut(printStream);
        log.info("CodeResult: {}", codeResult);

        // Ejecutar el código en un hilo separado para poder cancelarlo si se queda en un bucle infinito
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                compilationResult.getCompiler().getClassLoader().loadClass("Main")
                        .getMethod("main", String[].class)
                        .invoke(null, (Object) args);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e ) {
                executionResultDto.setExecution(false);
                executionResultDto.setMessage(executionFailedMessage + e.getMessage());
                throw new FailExecutionException(e.getMessage());
            }

        });

        try {
            future.get(5, TimeUnit.SECONDS); // Esperar 10 segundos
        } catch (InterruptedException | TimeoutException e) {
            executionResultDto.setExecution(false);
            executionResultDto.setMessage(executionFailedMessage + "Code execution timed out");
            return executionResult;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            executionResultDto.setExecution(false);
            executionResultDto.setMessage(executionFailedMessage + cause.getMessage());
            return executionResult;
        } finally {
            executor.shutdownNow();
        }


        System.out.flush();
        System.setOut(old);

        // Ejecución correcta
        executionResultDto.setExecution(true);
        executionResult.setExecutionResultMsg(removeTrailingNewline(outputStream.toString()));

        return executionResult;
    }

    public ExecutionResultDto compareResults(String result, String codeResult, ExecutionResultDto executionResultDto) {

        result = result.trim(); // Eliminar espacios en blanco alrededor del resultado, a veces aparecía "/r" al final del resultado y eso hace que la comparación falle.
        codeResult = codeResult.trim();

        log.info("CodeResult: {}", codeResult);

        if (result.equals(codeResult)) {
            executionResultDto.setResultCodeMatch(true);
            executionResultDto.setMessage("Code executed successfully, result matches expected result. Execution result: " + result);
        } else {
            executionResultDto.setResultCodeMatch(false);
            executionResultDto.setMessage("Code executed successfully, result does not match expected result. Execution result: " + result);
        }

        return executionResultDto;
    }

    public String[] castArgs(Object... args) {
        // Comprobar si args es nulo o vacío
        if (args == null || args.length == 0) {
            return new String[0];
        }

        // Comprobar si el primer elemento es un array
        if (args.length == 1 && args[0] instanceof Object[] innerArgs) {
            // Si es un array, tratarlo como tal
            String[] argsString = new String[innerArgs.length];
            for (int i = 0; i < innerArgs.length; i++) {
                if (innerArgs[i] == null) {
                    throw new IllegalArgumentException("Args cannot contain null");
                }
                argsString[i] = innerArgs[i].toString();
            }
            return argsString;
        } else {
            // Si no es un array, tratarlo como antes
            String[] argsString = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    throw new IllegalArgumentException("Args cannot contain null");
                }
                argsString[i] = args[i].toString();
            }
            return argsString;
        }
    }

    public static String removeTrailingNewline(String input) {
        // Si la cadena de entrada está vacía, devolverla tal cual
        if (input.isEmpty()) {
            return input;
        }
        //elimina el salto de línea al final del resultado si existe
        if (input.lastIndexOf("\n") == input.length() - 1) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }


    public int calculateScore(ExecutionResultDto executionResult) {
        int score = 0;

        if (executionResult.isCompiled() && executionResult.isExecution() && executionResult.isResultCodeMatch()) {
            score = 99;
            log.info("Score calculated: {}. \nReason: Code compiled, executed, and result matched.", score);
        } else if (executionResult.isCompiled() && executionResult.isExecution() && !executionResult.isResultCodeMatch()) {
            score = 75;
            log.info("Score calculated: {}. \nReason: Code compiled and executed, but result did not match.", score);
        } else if (executionResult.isCompiled() && !executionResult.isExecution()) {
            score = 50;
            log.info("Score calculated: {}.\nReason: Code compiled, but did not execute.", score);
        } else {

            log.info("Score calculated: {}. \nReason: Code did not compile.", score);
        }

        return score;
    }

}