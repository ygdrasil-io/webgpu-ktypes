#include <metal_stdlib>
using namespace metal;

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
    return (a % b);
}

float4 vs_main(float2 pos) {
    float local_0 = add_f32(pos[0], pos[0]);
    float local_1 = sub_f32(pos[0], pos[0]);
    float local_2 = mul_f32(pos[0], pos[0]);
    float local_3 = div_f32(pos[0], pos[0]);
    return float4(local_0, local_1, local_2, local_3);
}

struct vs_main_Output {
    float4 position [[position]];
};
[[vertex]]
vs_main_Output vs_main() {
}
