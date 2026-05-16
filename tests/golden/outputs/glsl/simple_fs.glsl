#version 450 core
precision highp float;
precision highp int;


vec4 fs_main() {
    return vec4(1.0f, 0.0f, 0.0f, 1.0f);
}

layout(location = 0) out vec4 outColor;
void main() {
    outColor = fs_main();
}
