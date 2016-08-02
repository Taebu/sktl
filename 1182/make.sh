rm -rf bin
mkdir bin
javac -cp ".:libs/*" -d bin $(find ./src/* | grep .java)
