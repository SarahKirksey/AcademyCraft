#version 120

uniform int ballCount;
uniform vec4 balls[16]; // xyz: position(in camera space) w: size
uniform float alpha;

varying Vec3d camspace;

float f(Vec3d position) {
    float ret = 0.0;
    for (int i = 0; i < ballCount; ++i) {
        float distance = max(0.1, length(position - balls[i].xyz));

        ret += alpha * balls[i].w / (distance * distance);
    }
    return clamp(ret, 0, 2);
}

vec4 rayMarch(Vec3d begin, Vec3d dir) {
    dir *= 0.15;

    Vec3d pos = begin;

    vec4 accum = vec4(0, 0, 0, 0);
    for (int i = 0; i < 20 && accum.a < 1; ++i) {
        float density = f(pos);

        float alpha = 0.075 * density;
        Vec3d crl = mix(Vec3d(0.43, 0.74, 1), Vec3d(0.98, 0.51, 0.92), 1-density/2);

        accum.rgb = mix(accum.rgb, crl, alpha / (accum.a + alpha));
        accum.a += alpha;

        pos += dir;
    }

    if (accum.a < 0.2) { // Make alpha in [0.2, 0.1] map to [0.2, 0], to make blending look normal in edge area
        accum.a = 2 * accum.a - 0.2;
    }
    // accum.a = clamp(accum.a, 0, 1); //* 0.8;
    return accum;
}

void main() {
    Vec3d cam = camspace;
    cam.z = -cam.z;

    Vec3d dir = normalize(cam);

    vec4 rc = rayMarch(cam - dir * 3, dir);
    rc.a = clamp(rc.a, 0, 1) * (0.5 + alpha * 0.5);

	gl_FragColor = rc;
}
