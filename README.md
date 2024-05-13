
# Image Compressor

[![](https://jitpack.io/v/xswapsx/ImageCompressorLibrary.svg)](https://jitpack.io/#xswapsx/ImageCompressorLibrary)

Compressor is a lightweight and powerful android image compression library. Compressor will allow you to compress large photos into smaller-sized photos with very little or negligible loss in the quality of the image.
# Gradle
Step 1. Add it to your root build.gradle at the end of repositories:

    allprojects{
    	repositories {

              ...
			maven { url 'https://jitpack.io' }
		}
	}
  
Step 2. Add it in your module's build.gradle:

dependencies {

      implementation 'com.github.xswapsx:ImageCompressorLibrary:{latest-release}'

}

# Let's compress the image size!
            
    val compressedImagePath = ImageCompressor.compressImage("Your file's absolute path")
    
   # desclaimer: 
        This library will overwrite the original file with the compressed file.

