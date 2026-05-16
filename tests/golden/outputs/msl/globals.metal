#include <metal_stdlib>
using namespace metal;
int global_0;
/* unknown type */ void global_1;

float4 vs_main(float3 pos) {
    
    global_0 = (global_0 + 1);
    return float(pos, 1.0f);
}

float4 fs_main() {
    
    return float(float(global_0), 0.0f, 0.0f, 1.0f);
}

void cs_main() {
    
    0 = 1.0f;
}

struct vs_main_Output {
    float4 position [[position]];
};
[[vertex]]
vs_main_Output vs_main() {
}

[[fragment]]
float4 fs_main() {
}

[[kernel]]
void cs_main() {
}
