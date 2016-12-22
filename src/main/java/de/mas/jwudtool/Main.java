package de.mas.jwudtool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import de.mas.jnus.lib.DecryptionService;
import de.mas.jnus.lib.ExtractionService;
import de.mas.jnus.lib.NUSTitle;
import de.mas.jnus.lib.NUSTitleLoaderWUD;
import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.WUDService;
import de.mas.jnus.lib.implementations.wud.WUDImage;
import de.mas.jnus.lib.utils.Utils;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("JWUDTool 0.1b - Maschell");
        System.out.println();
        Options options = getOptions();
        
        if (args.length == 0) {
            showHelp(options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try{
            cmd = parser.parse(options,  args);
        }catch(MissingArgumentException e){
            System.out.println(e.getMessage());
            return ;
        }catch(UnrecognizedOptionException e){
            System.out.println(e.getMessage());
            showHelp(options);
            return;
        }
        
        String input = null;
        String output = null;
        boolean overwrite = false;
        byte[] titlekey = null;
        
        readKey();
        
        if(cmd.hasOption("help")){
            showHelp(options);
            return;
        }
        
        if(cmd.hasOption("in")){
            input = cmd.getOptionValue("in");
        }
        if(cmd.hasOption("out")){
            output = cmd.getOptionValue("out");
        }
        
        if(cmd.hasOption("commonkey")){
            String commonKey = cmd.getOptionValue("commonkey");
            byte[] key = Utils.StringToByteArray(commonKey);
            if(key.length == 0x10){
                Settings.commonKey = key;
                System.out.println("Commonkey was set to: " + Utils.ByteArrayToString(key));
            }
        }
        
        if(cmd.hasOption("titlekey")){
            String titlekey_string = cmd.getOptionValue("titlekey");
            titlekey = Utils.StringToByteArray(titlekey_string);
            if(titlekey.length != 0x10){
                titlekey =  null;
            }else{
                System.out.println("Titlekey was set to: " + Utils.ByteArrayToString(titlekey));
            }
        }
        
        if(cmd.hasOption("overwrite")){
            overwrite = true;
        }
        
        if(cmd.hasOption("compress")){
            boolean verify = true;
            System.out.println("Compressing: " + input);
            if(cmd.hasOption("noVerify")){
                System.out.println("Verification disabled.");
                verify = false;
            }
            compressWUD(input,output,verify,overwrite);
            return;
        }else if(cmd.hasOption("verify")){
            System.out.println("Comparing images.");
            String[] verifyArgs = cmd.getOptionValues("verify");
            
            File input1 = new File(verifyArgs[0]);
            File input2 = new File(verifyArgs[1]);
            
            verifyImages(input1,input2);
            
            return;
        }else{
            if(cmd.hasOption("decrypt")){
                System.out.println("Decrypting full game partition.");
                
                decrypt(input,output,overwrite,titlekey);
                
                return;
            }else if(cmd.hasOption("decryptFile")){
                String regex = cmd.getOptionValue("decryptFile");
                System.out.println("Decrypting files matching \"" +regex + "\"");
                
                decryptFile(input,output,regex,overwrite,titlekey);
                
                return;
            }else if(cmd.hasOption("extract")){
                System.out.println("Extracting WUD");
                String arg = cmd.getOptionValue("extract");
                if(arg == null){
                    arg = "all";
                }
                extract(input,output,overwrite,titlekey,arg);
                
                return;
            }
        }
    }
    
    
    private static void extract(String input, String output, boolean overwrite, byte[] titlekey, String arg) throws Exception {
        if(input == null){
            System.out.println("You need to provide an input file");
        }
        boolean extractAll = false;
        boolean extractContent = false;
        boolean extractTicket = false;
        boolean extractHashes = false;
        
        switch(arg){
            case "all":
                extractAll = true;
                break;
            case "content":
                extractContent = true;
                break;
            case "ticket":
                extractTicket = true;
                break;
            case "hashes":
                extractHashes = true;
                break;
            default:
                System.out.println("Argument not found:" + arg);
                return;
             
        }
        
        File inputFile = new File(input);
        
        System.out.println("Extracting: " + inputFile.getAbsolutePath());
        
        NUSTitle title = NUSTitleLoaderWUD.loadNUSTitle(inputFile.getAbsolutePath(),titlekey);
        if(title == null){
            return;
        }
        
        if(output == null){
            output = String.format("%016X", title.getTMD().getTitleID());
        }else{
            output += File.separator + String.format("%016X", title.getTMD().getTitleID());
        }
        
        File outputFolder = new File(output);
        System.out.println("To the folder: " + outputFolder.getAbsolutePath());
        
        ExtractionService extractionService  = ExtractionService.getInstance(title);
        if(extractAll){
            extractionService.extractAll(outputFolder.getAbsolutePath());
        }else if(extractTicket){
            extractionService.extractTicketTo(outputFolder.getAbsolutePath());
        }else if(extractContent){
            extractionService.extractAllEncryptedContentFilesWithoutHashesTo(outputFolder.getAbsolutePath());
        }else if(extractHashes){
            extractionService.extractAllEncrpytedContentFileHashes(outputFolder.getAbsolutePath());
        }
        System.out.println("Extraction done!");
    }


    private static void decryptFile(String input, String output, String regex, boolean overwrite, byte[] titlekey) throws Exception {
        if(input == null){
            System.out.println("You need to provide an input file");
        }
        File inputFile = new File(input);
        
        System.out.println("Decrypting: " + inputFile.getAbsolutePath());
        
        NUSTitle title = NUSTitleLoaderWUD.loadNUSTitle(inputFile.getAbsolutePath(),titlekey);
        if(title == null){
            return;
        }
        
        
        if(output == null){
            output = String.format("%016X", title.getTMD().getTitleID());
        }else{
            output += File.separator + String.format("%016X", title.getTMD().getTitleID());
        }
        
        File outputFolder = new File(output);
        
        System.out.println("To the folder: " + outputFolder.getAbsolutePath());
        title.setSkipExistingFiles(!overwrite);
        DecryptionService decryption = DecryptionService.getInstance(title);
        
        
        decryption.decryptFSTEntriesTo(regex,outputFolder.getAbsolutePath());
        System.out.println("Decryption done");
    }


    private static void decrypt(String input,String output, boolean overwrite,byte[] titlekey) throws Exception {
        decryptFile(input,output,".*",overwrite,titlekey);
    }


    private static void verifyImages(File input1, File input2) throws IOException {
        System.out.println("Input 1: " +input1.getAbsolutePath());            
        WUDImage image1 = new WUDImage(input1);
        
        System.out.println("Input 2: " +input2.getAbsolutePath());         
        WUDImage image2 = new WUDImage(input2);
        if(WUDService.compareWUDImage(image1, image2)){
            System.out.println("Both images have the same data");
        }else{
            System.out.println("The images are different!");
        }        
    }


    private static void compressWUD(String input,String output, boolean verify, boolean overwrite) throws IOException {
        if(input == null){
            System.out.println("-in null");
            return;
        }
        File inputImage = new File(input);
        if(inputImage.isDirectory() || !inputImage.exists()){
            System.out.println(inputImage.getAbsolutePath() + " is no file or does not exist");
            return;
        }
        System.out.println("Parsing WUD image.");
        WUDImage image = new WUDImage(inputImage);
        File compressedImage = WUDService.compressWUDToWUX(image, output,overwrite);
        if(compressedImage != null){
            System.out.println("Compression successful!");
        }
        if(verify){
            if(compressedImage != null){
                WUDImage image2 = new WUDImage(compressedImage);
                if(WUDService.compareWUDImage(image, image2)){
                    System.out.println("Compressed files is valid.");
                }else{
                    System.out.println("Warning! Compressed file in INVALID!");
                }
            }
        }else{
            System.out.println("Verfication skipped");
        }
    }


    private static void readKey() throws IOException {
        File file = new File("common.key");
        if(file.isFile()){
            byte[] key = Files.readAllBytes(file.toPath());
            Settings.commonKey = key;
            System.out.println("Commonkey was set to: " + Utils.ByteArrayToString(key));
        }
    }


    private static Options getOptions() { // TODO: schöner machen?
        Options options = new Options();
        options.addOption(Option.builder("in").argName("input file").hasArg().desc("Input file. Can be a .wux, .wud or a game_part1.wud").build());
        options.addOption(Option.builder("out").argName("output path").hasArg().desc("The path where the result will be saved").build());
        options.addOption(Option.builder("compress").desc("Compresses the input to a .wux file.").build());
        options.addOption(Option.builder("noVerify").desc("Disables verification after compressing").build());
        options.addOption(Option.builder("verify").argName("wudimage1|wudimage2").hasArg().numberOfArgs(2).desc("Compares two WUD images to find differences").build());
        options.addOption(Option.builder("overwrite").desc("Optional. Overwrites existing files").build());
        options.addOption(Option.builder("commonkey").argName("WiiU common key").hasArg().desc("Optional. HexString. Will be used if no \"common.key\" in the folder of this .jar is found").build());
        options.addOption(Option.builder("decrypt").desc("Decrypts full the game partition of the given wud.").build());
        options.addOption(Option.builder("titlekey").argName("WUD title key").hasArg().desc("Optional. HexString. Will be used if no \"game.key\" in the folder of the wud image is found").build());
        options.addOption(Option.builder("decryptFile").argName("regular expression").hasArg().desc("Decrypts full the game partition of the given wud.").build());
        options.addOption(Option.builder("extract").argName("all|content|ticket|hashes").hasArg().optionalArg(true).desc("Extracts files from the game partition of the given wud (Arguments optional)").build());
        
        options.addOption("help", false, "shows this text");
        
        return options;
    }
    
    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp(" ", options);
    }

}
