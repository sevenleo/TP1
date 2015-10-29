all: 
	java src/Server.java 1500
	


push:
	git status
	git push https://github.com/sevenleo/TP1

pull:
	git status
	git pull https://github.com/sevenleo/TP1
