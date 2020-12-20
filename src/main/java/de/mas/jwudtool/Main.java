package de.mas.jwudtool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import de.mas.wiiu.jnus.DecryptionService;
import de.mas.wiiu.jnus.ExtractionService;
import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.WUDLoader;
import de.mas.wiiu.jnus.WUDService;
import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.WiiUDisc;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.val;

public class Main {
    private final static String OPTION_IN = "in";
    private final static String OPTION_HELP = "help";
    private static final String OPTION_OUT = "out";
    private static final String OPTION_DECOMPRESS = "decompress";
    private static final String OPTION_COMPRESS = "compress";
    private static final String OPTION_COMMON_KEY = "commonkey";
    private static final String OPTION_NO_VERIFY = "noVerify";
    private static final String OPTION_VERIFY = "verify";
    private static final String OPTION_OVERWRITE = "overwrite";
    private static final String OPTION_DECRYPT = "decrypt";
    private static final String OPTION_TITLEKEY = "titleKey";
    private static final String OPTION_DECRYPT_FILE = "decryptFile";
    private static final String OPTION_EXTRACT = "extract";
    private static final String OPTION_DEVMODE = "dev";
    private static byte[] commonKey;

    private static final String HOMEPATH = System.getProperty("user.home") + File.separator + ".wiiu";

    public static void main(String[] args) throws Exception {
        System.out.println("JWUDTool 0.4 - Maschell");
        System.out.println();
        Options options = getOptions();

        if (args.length == 0) {
            showHelp(options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (MissingArgumentException e) {
            System.out.println(e.getMessage());
            return;
        } catch (UnrecognizedOptionException e) {
            System.out.println(e.getMessage());
            showHelp(options);
            return;
        }

        String input = null;
        String output = null;
        boolean overwrite = false;
        boolean devMode = false;
        byte[] titlekey = null;

        readKey(new File(HOMEPATH + File.separator + "common.key")).ifPresent(key -> Main.commonKey = key);
        readKey(new File("common.key")).ifPresent(key -> Main.commonKey = key);

        if (cmd.hasOption(OPTION_HELP)) {
            showHelp(options);
            return;
        }

        if (cmd.hasOption(OPTION_IN)) {
            input = cmd.getOptionValue(OPTION_IN);
        }
        if (cmd.hasOption(OPTION_OUT)) {
            output = cmd.getOptionValue(OPTION_OUT);
        }

        if (cmd.hasOption(OPTION_COMMON_KEY)) {
            String commonKey = cmd.getOptionValue(OPTION_COMMON_KEY);
            byte[] key = Utils.StringToByteArray(commonKey);
            if (key.length == 0x10) {
                Main.commonKey = key;
                System.out.println("Commonkey was set to: " + Utils.ByteArrayToString(key));
            }
        }

        if (cmd.hasOption(OPTION_TITLEKEY)) {
            String titlekey_string = cmd.getOptionValue(OPTION_TITLEKEY);
            titlekey = Utils.StringToByteArray(titlekey_string);
            if (titlekey.length != 0x10) {
                titlekey = null;
            } else {
                System.out.println("Titlekey was set to: " + Utils.ByteArrayToString(titlekey));
            }
        }

        if (cmd.hasOption(OPTION_OVERWRITE)) {
            overwrite = true;
        }

        if (cmd.hasOption(OPTION_DEVMODE)) {
            devMode = true;
        }

        if (cmd.hasOption(OPTION_COMPRESS)) {
            boolean verify = true;
            System.out.println("Compressing: " + input);
            if (cmd.hasOption(OPTION_NO_VERIFY)) {
                System.out.println("Verification disabled.");
                verify = false;
            }
            compressDecompressWUD(input, output, verify, overwrite, false);
            return;
        } else if (cmd.hasOption(OPTION_DECOMPRESS)) {
            boolean verify = true;
            System.out.println("Decompressing: " + input);
            if (cmd.hasOption(OPTION_NO_VERIFY)) {
                System.out.println("Verification disabled.");
                verify = false;
            }
            compressDecompressWUD(input, output, verify, overwrite, true);
            return;
        } else if (cmd.hasOption(OPTION_VERIFY)) {
            System.out.println("Comparing images.");
            String[] verifyArgs = cmd.getOptionValues(OPTION_VERIFY);

            File input1 = new File(verifyArgs[0]);
            File input2 = new File(verifyArgs[1]);

            verifyImages(input1, input2);

            return;
        } else {
            if (cmd.hasOption(OPTION_DECRYPT)) {
                System.out.println("Decrypting full game partition.");

                decrypt(input, output, devMode, overwrite, titlekey);

                return;
            } else if (cmd.hasOption(OPTION_DECRYPT_FILE)) {
                String regex = cmd.getOptionValue(OPTION_DECRYPT_FILE);
                System.out.println("Decrypting files matching \"" + regex + "\"");

                decryptFile(input, output, regex, devMode, overwrite, titlekey);

                return;
            } else if (cmd.hasOption(OPTION_EXTRACT)) {
                System.out.println("Extracting WUD");
                String arg = cmd.getOptionValue(OPTION_EXTRACT);
                if (arg == null) {
                    arg = "all";
                }
                extract(input, output, devMode, overwrite, titlekey, arg);

                return;
            }
        }
    }

    private static void extract(String input, String output, boolean devMode, boolean overwrite, byte[] titlekey, String arg) throws Exception {
        if (input == null) {
            System.out.println("You need to provide an input file");
        }
        boolean extractAll = false;
        boolean extractContent = false;
        boolean extractTicket = false;
        boolean extractHashes = false;

        switch (arg) {
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

        WiiUDisc wudInfo = null;
        if (!devMode) {
            wudInfo = WUDLoader.load(inputFile.getAbsolutePath(), titlekey);
        } else {
            wudInfo = WUDLoader.loadDev(inputFile.getAbsolutePath());
        }

        if (wudInfo == null) {
            System.out.println("Failed to load WUX " + inputFile.getAbsolutePath());
            return;
        }

        List<NUSTitle> titles = WUDLoader.getGamePartionsAsNUSTitles(wudInfo, Main.commonKey);
        System.out.println("Found " + titles.size() + " titles on the Disc.");
        for (val title : titles) {
            String newOutput = output;
            System.out.println("Extract files of Title " + String.format("%016X", title.getTMD().getTitleID()));
            if (newOutput == null) {
                newOutput = String.format("%016X", title.getTMD().getTitleID());
            } else {
                newOutput += File.separator + String.format("%016X", title.getTMD().getTitleID());
            }

            File outputFolder = new File(newOutput);
            System.out.println("To the folder: " + outputFolder.getAbsolutePath());

            ExtractionService extractionService = ExtractionService.getInstance(title);
            if (extractAll) {
                extractionService.extractAll(outputFolder.getAbsolutePath());
            } else if (extractTicket) {
                extractionService.extractTicketTo(outputFolder.getAbsolutePath());
            } else if (extractContent) {
                extractionService.extractAllEncryptedContentFilesWithoutHashesTo(outputFolder.getAbsolutePath());
            } else if (extractHashes) {
                extractionService.extractAllEncrpytedContentFileHashes(outputFolder.getAbsolutePath());
            }
            System.out.println("Extraction done!");
        }

    }

    private static void decryptFile(String input, String output, String regex, boolean devMode, boolean overwrite, byte[] titlekey) throws Exception {
        if (input == null) {
            System.out.println("You need to provide an input file");
        }
        File inputFile = new File(input);

        System.out.println("Decrypting: " + inputFile.getAbsolutePath());

        WiiUDisc wudInfo = null;
        if (!devMode) {
            wudInfo = WUDLoader.load(inputFile.getAbsolutePath(), titlekey);
        } else {
            wudInfo = WUDLoader.loadDev(inputFile.getAbsolutePath());
        }

        if (wudInfo == null) {
            System.out.println("Failed to load Wii U Disc Image " + inputFile.getAbsolutePath());
            return;
        }

        List<FSTDataProvider> partitions = WUDLoader.getPartitonsAsFSTDataProvider(wudInfo, Main.commonKey);
        System.out.println("Found " + partitions.size() + " titles on the Disc.");

        Set<String> paritionNames = new TreeSet<>();
        for (val dp : partitions) {
            String partitionName = dp.getName();

            int i = 0;
            while (paritionNames.contains(partitionName)) {
                partitionName = dp.getName() + "_" + i++;
            }

            paritionNames.add(partitionName);

            String newOutput = output;
            System.out.println("Decrypting files in partition " + partitionName);
            if (newOutput == null) {
                newOutput = partitionName;
            } else {
                newOutput += File.separator + partitionName;
            }

            File outputFolder = new File(newOutput);

            System.out.println("To the folder: " + outputFolder.getAbsolutePath());
            DecryptionService decryption = DecryptionService.getInstance(dp);

            decryption.decryptFSTEntriesTo(regex, outputFolder.getAbsolutePath(), !overwrite);
        }
        System.out.println("Decryption done");
    }

    private static void decrypt(String input, String output, boolean devMode, boolean overwrite, byte[] titlekey) throws Exception {
        decryptFile(input, output, ".*", devMode, overwrite, titlekey);
    }

    private static void verifyImages(File input1, File input2) throws IOException {
        System.out.println("Input 1: " + input1.getAbsolutePath());
        WUDImage image1 = new WUDImage(input1);

        System.out.println("Input 2: " + input2.getAbsolutePath());
        WUDImage image2 = new WUDImage(input2);
        if (WUDService.compareWUDImage(image1, image2)) {
            System.out.println("Both images have the same data");
        } else {
            System.out.println("The images are different!");
        }
    }

    private static void compressDecompressWUD(String input, String output, boolean verify, boolean overwrite, boolean decompress) throws IOException {
        if (input == null) {
            System.out.println("-" + OPTION_IN + " was null");
            return;
        }
        File inputImage = new File(input);
        if (inputImage.isDirectory() || !inputImage.exists()) {
            System.out.println(inputImage.getAbsolutePath() + " is no file or does not exist");
            return;
        }
        System.out.println("Parsing WUD image.");
        WUDImage image = new WUDImage(inputImage);
        Optional<File> outputFile = Optional.empty();
        if (!decompress) {
            outputFile = WUDService.compressWUDToWUX(image, output, overwrite);
            if (outputFile.isPresent()) {
                System.out.println("Compression successful!");
            }
        } else {
            outputFile = WUDService.decompressWUX(image, output, overwrite);
            if (outputFile.isPresent()) {
                System.out.println("Decompression successful!");
            }
        }

        if (verify) {
            if (outputFile.isPresent()) {
                WUDImage image2 = new WUDImage(outputFile.get());
                if (WUDService.compareWUDImage(image, image2)) {
                    System.out.println("Compressed files is valid.");
                } else {
                    System.out.println("Warning! (De)Compressed file is INVALID!");
                }
            }
        } else {
            System.out.println("Verfication skipped");
        }
    }

    private static Optional<byte[]> readKey(File file) {
        if (file.isFile()) {
            byte[] key;
            try {
                key = Files.readAllBytes(file.toPath());
                if (key != null && key.length == 16) {
                    return Optional.of(key);
                }
            } catch (IOException e) {
            }
        }
        return Optional.empty();
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder(OPTION_IN).argName("input file").hasArg().desc("Input file. Can be a .wux, .wud or a game_part1.wud").build());
        options.addOption(Option.builder(OPTION_OUT).argName("output path").hasArg().desc("The path where the result will be saved").build());
        options.addOption(Option.builder(OPTION_DEVMODE).argName("dev mode").desc("Allows you to handle Kiosk Discs").build());
        options.addOption(Option.builder(OPTION_COMPRESS).desc("Compresses the input to a .wux file.").build());
        options.addOption(Option.builder(OPTION_DECOMPRESS).desc("Decompresses the input back to a .wud file.").build());
        options.addOption(Option.builder(OPTION_NO_VERIFY).desc("Disables verification after compressing").build());
        options.addOption(Option.builder(OPTION_VERIFY).argName("wudimage1|wudimage2").hasArg().numberOfArgs(2)
                .desc("Compares two WUD images to find differences").build());
        options.addOption(Option.builder(OPTION_OVERWRITE).desc("Optional. Overwrites existing files").build());
        options.addOption(Option.builder(OPTION_COMMON_KEY).argName("WiiU common key").hasArg()
                .desc("Optional. HexString. Will be used if no \"common.key\" in the folder of this .jar is found").build());
        options.addOption(Option.builder(OPTION_DECRYPT).desc("Decrypts full the game partition of the given wud.").build());
        options.addOption(Option.builder(OPTION_TITLEKEY).argName("WUD title key").hasArg()
                .desc("Optional. HexString. Will be used if no \"game.key\" in the folder of the wud image is found").build());
        options.addOption(
                Option.builder(OPTION_DECRYPT_FILE).argName("regular expression").hasArg().desc("Decrypts full the game partition of the given wud.").build());
        options.addOption(Option.builder(OPTION_EXTRACT).argName("all|content|ticket|hashes").hasArg().optionalArg(true)
                .desc("Extracts files from the game partition of the given wud (Arguments optional)").build());

        options.addOption(OPTION_HELP, false, "shows this text");

        return options;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp(" ", options);
    }

}
