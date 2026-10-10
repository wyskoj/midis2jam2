#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;
uniform vec4 m_MarkerColor;
uniform vec4 m_SungColor;
uniform vec4 m_UnsungColor;
uniform float m_WipeU0;
uniform float m_WipeU1;
uniform float m_WipeFraction;

varying vec2 texCoord;
varying vec4 vertColor;

// Half the width of the soft edge at the wipe, as a fraction of the glyph's width.
const float FEATHER = 0.04;

void main() {
    vec3 rgb = vertColor.rgb;

    // The glyph being sung is drawn in the marker colour: paint it sung up to the wipe, unsung after.
    vec3 fromMarker = abs(vertColor.rgb - m_MarkerColor.rgb);
    if (max(fromMarker.r, max(fromMarker.g, fromMarker.b)) < 0.01) {
        float across = (texCoord.x - m_WipeU0) / max(m_WipeU1 - m_WipeU0, 0.000001);
        float sung = 1.0 - smoothstep(m_WipeFraction - FEATHER, m_WipeFraction + FEATHER, across);
        rgb = mix(m_UnsungColor.rgb, m_SungColor.rgb, sung);
    }

    gl_FragColor = vec4(rgb, vertColor.a) * texture2D(m_ColorMap, texCoord);
}
