TwoLevel:
	clear
	clear
	javac -cp ".:mallet.jar:mallet-deps.jar:./*" TwoLevel.java

run:
	java -cp ".:mallet.jar:mallet-deps.jar:./*" TwoLevel .1	>> out.txt

clean:
	rm *.class
	rm out.txt
