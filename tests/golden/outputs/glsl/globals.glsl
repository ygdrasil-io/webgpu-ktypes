#version 450 core
precision highp float;
precision highp int;

 int global_0;
shared void global_1;

vec4 vs_main(vec3 pos) {
    
    global_0 = (global_0 + 1);
    return float(pos, 1.0f);
}

vec4 fs_main() {
    
    return float(float(global_0), 0.0f, 0.0f, 1.0f);
}

void cs_main() {
    
    0 = 1.0f;
}

void main() {
    gl_Position = vs_main(vec3(0.0));
}

layout(location = 0) out vec4 outColor;
void main() {
    outColor = fs_main();
}

void main() {
    cs_main();
}
