#version 450 core
precision highp float;
precision highp int;


float add_f32(float a, float b) {
    
    return (a + b);
}

float sub_f32(float a, float b) {
    
    return (a - b);
}

float mul_f32(float a, float b) {
    
    return (a * b);
}

float div_f32(float a, float b) {
    
    return (a / b);
}

float rem_f32(float a, float b) {
    
    return mod(a, b);
}

vec4 vs_main(vec2 pos) {
    float sum = add_f32(pos[0], pos[0]);
    float diff = sub_f32(pos[0], pos[0]);
    float prod = mul_f32(pos[0], pos[0]);
    float quot = div_f32(pos[0], pos[0]);
    
    float sum = add_f32(pos[0], pos[0]);
    float diff = sub_f32(pos[0], pos[0]);
    float prod = mul_f32(pos[0], pos[0]);
    float quot = div_f32(pos[0], pos[0]);
    return float(sum, diff, prod, quot);
}

void main() {
    gl_Position = vs_main(vec4(0.0));
}
