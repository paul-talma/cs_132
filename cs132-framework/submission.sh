#! zsh
build_path="build/distributions"
JAVA_HOME=/opt/homebrew/opt/openjdk@21 gradle build
tar -xf ${build_path}/704380248.tar -C ${build_path}
tar -czf ${build_path}/704380248.tar -C ${build_path} hw4 S2SV.java
