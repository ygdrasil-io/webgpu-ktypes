#include <metal_stdlib>
using namespace metal;
int global_0;

float4 vs_main(float3 pos) {
    global_0 = (global_0 + 1);
    return float4(pos, 1.0f);
}

struct vs_main_Output {
    float4 position [[position]];
};
[[vertex]]
vs_main_Output vs_main() {
}
