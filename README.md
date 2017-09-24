# myilibrary-dedrm
Removes DRM &amp; downloads books from MyiLibrary

I wrote this tool because MyiLibrary doesn't allow you to download books, and provides a completely and utterly unusable interface for viewing books which is both laggy and just generally awful - you can't even scroll properly.

To use this tool:
* Firstly download the JAR file from here: https://www.dropbox.com/s/1rcqecapgcvlp6s/myilibrary.jar?dl=0
* Next, launch the application and navigate to the MyiLibrary viewer for the book which you would like to download. If you need to type in a URL, use the URL box at the top and hit enter to navigate.
* Now just enter a file name, and click 'Download Book'. The book will begin downloading, and once complete will open in your default PDF viewer.

If the file generated is too large:
* This tool generates large files because of the fact that it downloads images which represent the book which you want to download and then compiles them into a PDF.
* The best way to remedy this is to use Adobe Acrobat. Open the PDF, select 'Enhance Scans', and allow the process to complete. I've seen a 250mb PDF go down to ~11mb. This is achieved through OCR.
* If you do not have Adobe Acrobat, an alternative is to go to https://smallpdf.com/ and use their service. This won't be so effective, but even so can yield files up to 50% smaller.
