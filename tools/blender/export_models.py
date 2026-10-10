"""Export midis2jam2's model sources (.blend) to the .glb files the build converts. See docs/ASSETS.md.

Every .blend under sharedAssets/models is the source for the .glb beside it, which is committed with it. Export with
this script rather than by hand, so every file is exported the same way, and stamped with its .blend's SHA-256 (tests
fail when a .blend has changed since its .glb was exported). `./gradlew exportModels` runs this for every .blend whose
.glb is out of date:

  # every .blend under sharedAssets/models
  blender -b --factory-startup --python-exit-code 1 --python tools/blender/export_models.py -- sharedAssets/models

  # just the .blend files named
  blender -b --factory-startup --python-exit-code 1 --python tools/blender/export_models.py -- sharedAssets/models/Reed/Sax/Alto.blend

Inside Blender, open this script in the Text Editor and Run Script to export the open file (save it first). Don't
export through File > Export: that .glb isn't stamped, so the tests will ask for it to be exported again.
"""
import hashlib
import os
import sys

import bpy


# The scene property the .glb carries its source's SHA-256 in. The build's tests compare it with the .blend, so a
# .blend changed but not re-exported fails them (see ModelSourcesTest).
SOURCE_PROPERTY = "source_sha256"


def export_glb(path):
    """Export the open file to the .glb at `path`, stamped with the saved .blend's SHA-256.

    Images aren't embedded: the build finds each texture under sharedAssets/Assets by the material's name (or its
    `look` custom property), so the .glb only needs the material names. Custom properties carry the `look`s.
    """
    if not bpy.data.filepath:
        raise RuntimeError("Save the .blend before exporting it")
    if bpy.data.is_dirty:
        raise RuntimeError(f"{bpy.data.filepath} has unsaved changes; save it before exporting")
    with open(bpy.data.filepath, "rb") as source:
        bpy.context.scene[SOURCE_PROPERTY] = hashlib.sha256(source.read()).hexdigest()
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format="GLB",
        export_yup=True,
        export_apply=True,
        export_materials="EXPORT",
        export_image_format="NONE",
        export_extras=True,
        export_texcoords=True,
        export_normals=True,
        export_tangents=False,
        use_selection=False,
        use_visible=False,
    )


def sources(arguments):
    for argument in arguments:
        if os.path.isdir(argument):
            for root, _, files in os.walk(argument):
                yield from (os.path.join(root, f) for f in sorted(files) if f.endswith(".blend"))
        else:
            yield argument


def main(arguments, log):
    if not arguments and bpy.data.filepath:
        arguments = [bpy.data.filepath]  # run from inside Blender: export the open file
    for blend in sources(arguments):
        bpy.ops.wm.open_mainfile(filepath=blend)
        glb = os.path.splitext(blend)[0] + ".glb"
        export_glb(glb)
        log(f"exported {glb}")


if __name__ == "__main__":
    # Blender's output is lost when it runs through the Microsoft Store launcher, so the Gradle task (exportModels)
    # passes a file to report to as well.
    report = os.environ.get("MIDIS2JAM2_EXPORT_LOG")

    def log(message):
        print(message)
        if report:
            with open(report, "a", encoding="utf-8") as f:
                f.write(message + "\n")

    try:
        main(sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else [], log)
    except Exception as e:
        import traceback
        log(traceback.format_exc())
        sys.exit(1)
