#version 450 core
precision highp float;
precision highp int;


vec4 vs_main() {
    float x = 0;
    return vec4(0, x);
}

void main() {
    gl_Position = vs_main();
}
