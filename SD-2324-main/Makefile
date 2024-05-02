JAVAC = javac
CLASS_FILES = ./*.class
# view/*.class model/Businesses/*.class model/Reviews/*.class model/Users/*.class loaders/*.class

#Default: $(CLASS_FILES)
.DEFAULT_GOAL = build

build: 
	$(JAVAC) -cp sd23.jar -d out src/*.java

.PHONY: client # Run client
client: 
	java -cp sd23.jar:out InterfaceUser

.PHONY: server # Run server
server: 
	java -cp sd23.jar:out Server

.PHONY: clean # Clean java class files
clean:
	@rm -r out

.PHONY: man, help # Generate list of targets with descriptions
man help:
	@echo -------------------------------------------------------------------+
	@grep '^.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/    \1 \t: \2\n-------------------------------------------------------------------+/'