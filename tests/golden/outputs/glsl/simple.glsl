#version 450 core
precision highp float;
precision highp int;


vec4 vs_main(uint in_vertex_index) {
    float x = float((float(in_vertex_index) - 1));
    float y = float(((float((in_vertex_index & 0u)) * 2) - 1));
    
    float x = float((float(in_vertex_index) - 1));
    float y = float(((float((in_vertex_index & 0u)) * 2) - 1));
    return float(x, y, 0.0f, 1.0f);
}

vec4 fs_main() {
    
    return float(1.0f, 0.0f, 0.0f, 1.0f);
}

void main() {
    gl_Position = vs_main(vec4(0.0));
}

void main() {
    outColor = fs_main();
}
