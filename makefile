# Marco Martinez
# Computer Networks I
# Web Server
# makefile

server: WebServerMain.java MainThread.java Worker.java
	javac MainThread.java
	javac Worker.java
	javac WebServerMain.java
	java WebServerMain