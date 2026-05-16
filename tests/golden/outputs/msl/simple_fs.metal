#include <metal_stdlib>
using namespace metal;

float4 fs_main() {
    return float4(1.0f, 0.0f, 0.0f, 1.0f);
}

[[fragment]]
float4 fs_main() {
}
