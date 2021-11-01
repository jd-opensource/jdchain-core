package com.jd.blockchain.tools.cli;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.function.IntPredicate;

public class ScannerUtils {

    public static final String DEFAULT_PROMPT = "> ";

    private static final Scanner SCANNER = new Scanner(new InputStreamReader(System.in, Charset.defaultCharset()));

    private ScannerUtils() {

    }

    public static String read(String prompt){
        if(prompt != null){
            System.out.print(prompt);
            System.out.flush();
        }
        return SCANNER.nextLine().trim();
    }

    public static String read(){
        return read(DEFAULT_PROMPT);
    }

    public static int readInt(){
        return readInt(DEFAULT_PROMPT, null, i -> true);
    }

    public static int readInt(String prompt){
        return readInt(prompt, null, i -> true);
    }

    public static int readInt(String prompt, String errPrompt){
        return readInt(prompt, errPrompt, i -> true);
    }

    public static int readRangeInt(int min, int max){
        return readRangeInt(DEFAULT_PROMPT, min, max);
    }

    public static int readRangeInt(String prompt, int min, int max){
        return readInt(prompt, String.format("outside the selected range: [%d,%d]", min, max), i -> i >= min && i <= max);
    }

    public static int readRangeInt(String prompt, String errPrompt, int min, int max){
        return readInt(prompt, errPrompt, i -> i >= min && i <= max);
    }

    private static int readInt(String prompt, String errPrompt, IntPredicate integerPredicate) {
        Integer value = null;

        while (value == null) {
            String input = read(prompt);
            try{
                int tmp =  Integer.parseInt(input);
                value = integerPredicate == null || integerPredicate.test(tmp) ? tmp : null;

            }catch (Exception e) {
                //ignore
            }

            if(value == null && errPrompt != null){
                System.err.println(errPrompt);
            }
        }

        return value;
    }

}
