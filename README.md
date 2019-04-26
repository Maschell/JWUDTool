# JWUDTool 0.2

Here is just a simple program that uses the (http://gbatemp.net/threads/jnuslib-java-nus-library.452954/).  
The usage should be pretty self explaining.

**STILL EXPERIMENTAL. Bugs may occur, please report them!**

## Features

* Compressing .wud and splitted wud files into .wux
* Decompressing a .wux back to .wud
* Extracting from the GI or GM partition
* Extracting .app/-h3/.tmd/.cert/.tik files from a .wud/.wux or splitted .wud
* Extracting just the contents/hashes/ticket.
* Decrypting the full game partition from a .wud/.wux or splitted .wud
* Decrypting specific files the game partition from a .wud/.wux or splitted .wud
* Verify a image / Compare two images (for example a .wud with .wux to make sure its legit)

## Usage

Optional:
- Copy the common.key into the folder next to the .jar or provide the key via the command line
- Copy the game.key into the folder next to the wud image or provide the key via the command line

```
usage:
 -commonkey <WiiU common key>           Optional. HexString. Will be used if no "common.key" in the
                                        folder of this .jar is found
 -dev                                   Required when using discs without a titlekey.
 -compress                              Compresses the input to a .wux file.
 -decompress                            Decompresses the input to a .wud file.
 -decrypt                               Decrypts full the game partition of the given wud.
 -decryptFile <regular expression>      Decrypts files of the game partition that match the regular
                                        expression of the given wud.
 -extract <all|content|ticket|hashes>   Extracts files from the game partition of the given wud
                                        (Arguments optional)
 -help                                  shows this text
 -in <input file>                       Input file. Can be a .wux, .wud or a game_part1.wud
 -noVerify                              Disables verification after (de)compressing
 -out <output path>                     The path where the result will be saved
 -overwrite                             Optional. Overwrites existing files
 -titlekey <WUD title key>              Optional. HexString. Will be used if no "game.key" in the
                                        folder of the wud image is found
 -verify <wudimage1|wudimage2>          Compares two WUD images to find differences
 ```
# Examples
## Getting .app files from an Wii U Image:
### Extract .app etc. from a WUD:
Get .app files from "game.wud" to the folder "extracted" with game.key in the same folder
```
java -jar JWUDTool.jar -in "game.wud" -out "extracted" -extract all
```

### Extract .app etc. from a WUX (compressed WUD):
Get .app files from "game.wux" to the folder "extracted" with game.key in the same folder
```
java -jar JWUDTool.jar -in "game.wux" -out "extracted" -extract all
```

### Extract .app etc. from a splitted WUD (dump with wudump):
Get .app files from "game_part1.wud" to the folder "extracted" with game.key in the same folder
```
java -jar JWUDTool.jar -in "game_part1.wud" -out "extracted" -extract all
```

## Compressing into .wux examples:
### Compress a .wud to .wux:[/B]
Compress a "game.wud" to "game.wux"
```
java -jar JWUDTool.jar -in "game.wud" -compress
```

### Compress a splitted game_part1.wud to .wux:
Compress a "game_part1.wud" from a wudump dump to "game.wux"
```
java -jar JWUDTool.jar -in "game_part1.wud" -compress
```

## Decryption game files examples:
### Decrypt a WUD image to game files
Input can be a .wud, game_part1.wud or a .wux. This decrypted the full game partition.
Given a game.key and common.key in the same folder.
```
java -jar JWUDTool.jar -in "game.wud" -decrypt //WUD
java -jar JWUDTool.jar -in "game.wux" -decrypt //WUX
java -jar JWUDTool.jar -in "game_part1.wud" -decrypt //game_part1
```

### Decrypt a single file from an WUD
Input can be a .wud, game_part1.wud or a .wux. This decrypted the full game partition.
Given a game.key and common.key in the same folder.

Extracting the code/app.xml file.
```
java -jar JWUDTool.jar -in "game.wud" -decryptFile /code/app.xml
java -jar JWUDTool.jar -in "game.wux" -decryptFile /code/app.xml
java -jar JWUDTool.jar -in "game_part1.wud" -decryptFile /code/app.xml
```

Extracting all .bfstm files.
```
java -jar JWUDTool.jar -in "game.wud" -decryptFile /.*.bfstm
java -jar JWUDTool.jar -in "game.wux"  -decryptFile /.*.bfstm
java -jar JWUDTool.jar -in "game_part1.wud" -decryptFile /.*.bfstm
```

Extracting the folder /content/Sound
```
java -jar JWUDTool.jar -in "game.wud" -decryptFile /content/Sound/.*
java -jar JWUDTool.jar -in "game.wux"  -decryptFile /content/Sound/.*
java -jar JWUDTool.jar -in "game_part1.wud" -decryptFile /content/Sound/.*
```
 
## Compiling
`mvn clean package`

## Credits
Maschell  

Thanks to:  
Crediar for CDecrypt (https://github.com/crediar/cdecrypt)  
All people who have contributed to vgmtoolbox (https://sourceforge.net/projects/vgmtoolbox/)  
Exzap for the .wux file format (https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/)  
FIX94 for wudump (https://gbatemp.net/threads/wudump-dump-raw-images-from-a-wiiu-game-disc.451736/)  
The creators of lombok for lombok https://projectlombok.org/index.html  
