import os
import tempfile
import logging
import numpy as np

from OCC.Core.STEPControl import STEPControl_Reader
from OCC.Core.IFSelect import IFSelect_RetDone
from OCC.Core.BRepMesh import BRepMesh_IncrementalMesh
from OCC.Core.TopExp import TopExp_Explorer
from OCC.Core.TopAbs import TopAbs_FACE
from OCC.Core.BRep import BRep_Tool
from OCC.Core.TopLoc import TopLoc_Location
import trimesh

log = logging.getLogger(__name__)


def step_to_glb(step_bytes: bytes) -> bytes:
    """Convert STEP file bytes to GLB bytes."""

    # Write STEP to a temp file (OCC requires a file path)
    with tempfile.NamedTemporaryFile(suffix='.step', delete=False) as f:
        f.write(step_bytes)
        step_path = f.name

    try:
        # --- Read STEP ---
        reader = STEPControl_Reader()
        status = reader.ReadFile(step_path)
        if status != IFSelect_RetDone:
            raise ValueError(f"Failed to read STEP file (status={status})")

        reader.TransferRoots()
        shape = reader.OneShape()

        # --- Tessellate ---
        # deflection=0.1mm (linear), angDeflection=0.5rad
        mesh_algo = BRepMesh_IncrementalMesh(shape, 0.1, False, 0.5, True)
        mesh_algo.Perform()
        if not mesh_algo.IsDone():
            raise ValueError("Tessellation failed")

        # --- Extract mesh data from all faces ---
        all_vertices = []
        all_faces = []
        vertex_offset = 0

        explorer = TopExp_Explorer(shape, TopAbs_FACE)
        while explorer.More():
            face = explorer.Current()
            location = TopLoc_Location()
            triangulation = BRep_Tool.Triangulation(face, location)

            if triangulation is not None and triangulation.NbTriangles() > 0:
                nb_nodes = triangulation.NbNodes()
                nb_tris = triangulation.NbTriangles()

                for i in range(1, nb_nodes + 1):
                    p = triangulation.Node(i)
                    all_vertices.append([p.X(), p.Y(), p.Z()])

                for i in range(1, nb_tris + 1):
                    tri = triangulation.Triangle(i)
                    n1, n2, n3 = tri.Get()
                    all_faces.append([
                        n1 - 1 + vertex_offset,
                        n2 - 1 + vertex_offset,
                        n3 - 1 + vertex_offset,
                    ])

                vertex_offset += nb_nodes

            explorer.Next()

        if not all_vertices or not all_faces:
            raise ValueError("No geometry found in STEP file")

        log.info(f"Extracted {len(all_vertices)} vertices, {len(all_faces)} triangles")

        # --- Build trimesh and export as GLB ---
        vertices = np.array(all_vertices, dtype=np.float32)
        faces = np.array(all_faces, dtype=np.int32)

        mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=True)
        glb_bytes = mesh.export(file_type='glb')

        if not isinstance(glb_bytes, bytes):
            glb_bytes = bytes(glb_bytes)

        return glb_bytes

    finally:
        try:
            os.unlink(step_path)
        except OSError:
            pass
