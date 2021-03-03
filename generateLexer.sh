cd src/main/java/com/tang/intellij/lua
java -jar jflex-1.7.0.jar -skel idea-flex.skeleton -d ../../../../../../../gen/com/tang/intellij/lua/lexer lua.flex
java -jar jflex-1.7.0.jar -skel idea-flex.skeleton -d ../../../../../../../gen/com/tang/intellij/lua/lexer region.flex
java -jar jflex-1.7.0.jar -skel idea-flex.skeleton -d ../../../../../../../gen/com/tang/intellij/lua/lexer string.flex
java -jar jflex-1.7.0.jar -skel idea-flex.skeleton -d ../../../../../../../gen/com/tang/intellij/lua/comment/lexer doc.flex
cd -