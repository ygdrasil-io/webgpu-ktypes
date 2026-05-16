#include <metal_stdlib>
using namespace metal;

float4 vs_main(uint in_vertex_index) {
    float local_0 = float((float(in_vertex_index) - 1));
    float local_1 = float(((float((in_vertex_index & 0u)) * 2) - 1));
    
    float local_0 = float((float(in_vertex_index) - 1));
    float local_1 = float(((float((in_vertex_index & 0u)) * 2) - 1));
    return float(local_0, local_1, 0.0f, 1.0f);
}

float4 fs_main() {
    
    return float(1.0f, 0.0f, 0.0f, 1.0f);
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
