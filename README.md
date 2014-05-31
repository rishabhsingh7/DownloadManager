DownloadManager
===============

Download Manager in Java to download multiple files in parts parallely


Execution Logic

•	Take a new URL from the user.
•	Check URL for validity.

•	Spawn a new thread for each new URL.

•	Check if server allows division of files in parts.

  o	If server permits, divide the file into a specified number of parts and download each part separately in its own thread.

  o	If it does not, download the complete file in this thread only.

•	Display file’s progress (combined progress in case of downloading in parts) in the main GUI screen.

•	Perform pause/resume or stop functions if user requests.

•	Upon completion, join the parts in correct order to get the final file and exit all threads properly.

•	Wait for another URL.


RUN:
Gui.java
