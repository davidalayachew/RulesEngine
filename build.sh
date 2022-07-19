
if test -f ".gitignore"; then
    echo ".gitignore already exists, so doing nothing."
else
	echo "/target" > .gitignore
	echo "/ignore" >> .gitignore
fi


mvn clean install exec:java