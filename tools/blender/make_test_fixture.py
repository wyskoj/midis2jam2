"""Builds :asset-tools' test fixture, asset-tools/src/test/resources/fixture.glb, covering each material rule.

  blender -b --factory-startup --python-exit-code 1 --python tools/blender/make_test_fixture.py -- \
      asset-tools/src/test/resources/fixture.glb tools/blender/export_models.py

  Thing (empty)          a model of two parts
    Body  -> material "Red"     (named like a library material)
    Label -> material "pic"     (named like a texture: pic.png)
  Plain                  a one-part model
          -> material "Anything" with custom property look = "reflective pic.png"
  Turned                 a one-part model, rotated 90 degrees about X and scaled 2x along its own Y
  Shade                  a one-part model
          -> material "Shade" with custom property look = "shadow pic.png"

Each mesh is one quad whose first corner has UV (0, 0.25) in Blender, so the test can check UVs come out as Blender
has them.
"""
import os
import sys
import tempfile

import bpy

out = sys.argv[sys.argv.index("--") + 1]
export = {"__name__": "export_models"}
exec(open(sys.argv[sys.argv.index("--") + 2]).read(), export)

bpy.ops.wm.read_factory_settings(use_empty=True)


def quad(name, x, material):
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata([(x, 0, 0), (x + 1, 0, 0), (x + 1, 1, 0), (x, 1, 0)], [], [(0, 1, 2, 3)])
    uv = mesh.uv_layers.new()
    for loop, value in zip(mesh.polygons[0].loop_indices, [(0, 0.25), (1, 0.25), (1, 1), (0, 1)]):
        uv.data[loop].uv = value
    mesh.materials.append(material)
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    return obj


def material(name, look=None):
    mat = bpy.data.materials.new(name)
    if look:
        mat["look"] = look
    return mat


thing = bpy.data.objects.new("Thing", None)
bpy.context.scene.collection.objects.link(thing)
quad("Body", 0, material("Red")).parent = thing
quad("Label", 2, material("pic")).parent = thing
quad("Plain", 4, material("Anything", "reflective pic.png"))
# Turned like Blender's OBJ import leaves objects, and stretched: the build must bake this into the vertices.
turned = quad("Turned", 6, material("Red"))
turned.rotation_euler = (1.5707963, 0, 0)
turned.scale = (1, 2, 1)
quad("Shade", 8, material("Shade", "shadow pic.png"))

bpy.ops.wm.save_as_mainfile(filepath=os.path.join(tempfile.mkdtemp(), "fixture.blend"))  # export needs a saved file
export["export_glb"](out)
