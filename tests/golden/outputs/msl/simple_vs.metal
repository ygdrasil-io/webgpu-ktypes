#include <metal_stdlib>
using namespace metal;

float4 vs_main(uint in_vertex_index) {
    float local_0 = float((int(in_vertex_index) - 1));
    float local_1 = float(((int((in_vertex_index & 0u)) * 2) - 1));
    return float4(local_0, local_1, 0.0f, 1.0f);
}

struct vs_main_Output {
    float4 position [[position]];
};
[[vertex]]
vs_main_Output vs_main() {
}
