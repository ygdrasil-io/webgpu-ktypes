#version 450 core
precision highp float;
precision highp int;


vec4 vs_main(uint in_vertex_index) {
    float x = float((int(in_vertex_index) - 1));
    float y = float(((int((in_vertex_index & 0u)) * 2) - 1));
    return vec4(x, y, 0.0f, 1.0f);
}

void main() {
    gl_Position = vs_main(uint(0.0));
}
