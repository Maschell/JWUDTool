#JWUDTool 0.1

Here is just a simple program that uses the (http://gbatemp.net/threads/jnuslib-java-nus-library.452954/).  
The usage should be pretty self explaining.

**STILL EXPERIMENTAL. Bugs may occur, please report them!**

##Features

* Compressing .wud and splitted wud files into .wux
* Extracting .app/-h3/.tmd/.cert/.tik files from a .wud/.wux or splitted .wud
* Exctracting just the contents/hashes/ticket.
* Decrypting the full game partition from a .wud/.wux or splitted .wud
* Decrypting specific files the game partition from a .wud/.wux or splitted .wud
* Verify a image / Compare two images (for example a .wud with .wux to make sure its legit)

##Usage

Optional:
- Copy the common.key into the folder next to the .jar or provide the key via the command line
- Copy the game.key into the folder next to the wud image or provide the key via the command line

```
usage:
 -commonkey <WiiU common key>           Optional. HexString. Will be used if no "common.key" in the
                                        folder of this .jar is found
 -compress                              Compresses the input to a .wux file.
 -decrypt                               Decrypts full the game partition of the given wud.
 -decryptFile <regular expression>      Decrypts files of the game partition that match the regular
                                        expression of the given wud.
 -extract <all|content|ticket|hashes>   Extracts files from the game partition of the given wud
                                        (Arguments optional)
 -help                                  shows this text
 -in <input file>                       Input file. Can be a .wux, .wud or a game_part1.wud
 -noVerify                              Disables verification after compressing
 -out <output path>                     The path where the result will be saved
 -overwrite                             Optional. Overwrites existing files
 -titlekey <WUD title key>              Optional. HexString. Will be used if no "game.key" in the
                                        folder of the wud image is found
 -verify <wudimage1|wudimage2>          Compares two WUD images to find differences
 ```
 
##Compiling
Add the "jnuslib.jar" into the library path and load the other dependicies throuugh maven.

##Credits
Maschell  

Thanks to:  
Crediar for CDecrypt (https://github.com/crediar/cdecrypt)  
All people who have contributed to vgmtoolbox (https://sourceforge.net/projects/vgmtoolbox/)  
Exzap for the .wux file format (https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/)  
FIX94 for wudump (https://gbatemp.net/threads/wudump-dump-raw-images-from-a-wiiu-game-disc.451736/)  
The creators of lombok for lombok https://projectlombok.org/index.html  
