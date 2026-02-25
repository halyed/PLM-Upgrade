import {
  Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, signal
}  from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { DocumentService } from '../../core/services/document.service';

interface TreeNode {
  name: string;
  object: THREE.Object3D;
  children: TreeNode[];
  visible: boolean;
}

@Component({
  selector: 'app-viewer',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatCardModule,
    MatTooltipModule,
  ],
  templateUrl: './viewer.component.html',
  styles: [`
    :host { display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
    .viewer-toolbar {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 16px; background: #1a1a2e; color: white;
      flex-shrink: 0;
    }
    .viewer-body { display: flex; flex: 1; overflow: hidden; }
    .tree-panel {
      width: 240px; background: #16213e; color: #e0e0e0;
      overflow-y: auto; padding: 8px; flex-shrink: 0;
    }
    .tree-panel h3 { margin: 8px 0; font-size: 13px; color: #90caf9; text-transform: uppercase; letter-spacing: 1px; }
    .tree-node {
      padding: 4px 8px; border-radius: 4px; cursor: pointer;
      font-size: 13px; display: flex; align-items: center; gap: 6px;
      transition: background 0.15s;
    }
    .tree-node:hover { background: rgba(255,255,255,0.1); }
    .tree-node.selected { background: #1565c0; }
    .tree-node.hidden { opacity: 0.4; }
    canvas { flex: 1; display: block; outline: none; }
    .overlay {
      position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
      text-align: center; color: white;
    }
  `],
})
export class ViewerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  loading = signal(true);
  error = signal('');
  fileName = signal('');
  objectTree = signal<TreeNode[]>([]);
  selectedNode = signal<TreeNode | null>(null);

  private renderer!: THREE.WebGLRenderer;
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private controls!: OrbitControls;
  private animFrameId = 0;
  private raycaster = new THREE.Raycaster();
  private mouse = new THREE.Vector2();
  private highlightMaterial = new THREE.MeshStandardMaterial({ color: 0xffaa00, emissive: 0x553300 });
  private originalMaterials = new Map<THREE.Mesh, THREE.Material | THREE.Material[]>();
  private resizeObserver!: ResizeObserver;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService,
  ) {}

  ngOnInit() {
    const docId = Number(this.route.snapshot.paramMap.get('documentId'));
    this.documentService.getDownloadUrl(docId).subscribe({
      next: ({ url }) => this.loadGltf(url),
      error: () => this.error.set('Failed to get file URL'),
    });
  }

  ngAfterViewInit() {
    this.initThree();
  }

  private initThree() {
    const canvas = this.canvasRef.nativeElement;
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1;

    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x1a1a2e);
    this.scene.fog = new THREE.Fog(0x1a1a2e, 50, 200);

    const w = canvas.clientWidth, h = canvas.clientHeight;
    this.camera = new THREE.PerspectiveCamera(45, w / h, 0.01, 1000);
    this.camera.position.set(5, 5, 5);

    this.controls = new OrbitControls(this.camera, canvas);
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.05;

    const ambient = new THREE.AmbientLight(0xffffff, 0.6);
    this.scene.add(ambient);
    const dirLight = new THREE.DirectionalLight(0xffffff, 1.5);
    dirLight.position.set(10, 20, 10);
    dirLight.castShadow = true;
    this.scene.add(dirLight);
    const fillLight = new THREE.DirectionalLight(0x88aaff, 0.4);
    fillLight.position.set(-10, 0, -10);
    this.scene.add(fillLight);

    const grid = new THREE.GridHelper(20, 20, 0x444466, 0x333355);
    this.scene.add(grid);

    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.resizeObserver.observe(canvas.parentElement);
    this.onResize();

    canvas.addEventListener('click', this.onCanvasClick.bind(this));
    this.animate();
  }

  private loadGltf(url: string) {
    const loader = new GLTFLoader();
    loader.load(
      url,
      (gltf) => {
        const model = gltf.scene;
        const box = new THREE.Box3().setFromObject(model);
        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z);
        const scale = 5 / maxDim;
        model.scale.setScalar(scale);
        model.position.sub(center.multiplyScalar(scale));

        model.traverse(obj => {
          if (obj instanceof THREE.Mesh) {
            obj.castShadow = true;
            obj.receiveShadow = true;
          }
        });

        this.scene.add(model);
        this.camera.position.set(maxDim * scale * 1.5, maxDim * scale, maxDim * scale * 1.5);
        this.controls.target.set(0, 0, 0);
        this.controls.update();

        this.objectTree.set(this.buildTree(model));
        this.loading.set(false);
      },
      undefined,
      (err) => {
        console.error(err);
        this.error.set('Failed to load model. Ensure it is a valid glTF/GLB file.');
        this.loading.set(false);
      }
    );
  }

  private buildTree(obj: THREE.Object3D): TreeNode[] {
    return obj.children
      .filter(c => c.name || c instanceof THREE.Mesh || c.children.length > 0)
      .map(c => ({
        name: c.name || (c instanceof THREE.Mesh ? 'Mesh' : 'Group'),
        object: c,
        visible: c.visible,
        children: this.buildTree(c),
      }));
  }

  selectNode(node: TreeNode) {
    const prev = this.selectedNode();
    if (prev) this.restoreHighlight(prev.object);
    this.selectedNode.set(node);
    this.applyHighlight(node.object);
    const box = new THREE.Box3().setFromObject(node.object);
    if (!box.isEmpty()) {
      const center = box.getCenter(new THREE.Vector3());
      const size = box.getSize(new THREE.Vector3()).length();
      this.controls.target.copy(center);
      this.camera.position.copy(center).add(new THREE.Vector3(size, size, size));
      this.controls.update();
    }
  }

  toggleVisibility(node: TreeNode, event: Event) {
    event.stopPropagation();
    node.object.visible = !node.object.visible;
    node.visible = node.object.visible;
    this.objectTree.update(t => [...t]);
  }

  private applyHighlight(obj: THREE.Object3D) {
    obj.traverse(child => {
      if (child instanceof THREE.Mesh) {
        this.originalMaterials.set(child, child.material);
        child.material = this.highlightMaterial;
      }
    });
  }

  private restoreHighlight(obj: THREE.Object3D) {
    obj.traverse(child => {
      if (child instanceof THREE.Mesh && this.originalMaterials.has(child)) {
        child.material = this.originalMaterials.get(child);
        this.originalMaterials.delete(child);
      }
    });
  }

  private onCanvasClick(event: MouseEvent) {
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
    this.raycaster.setFromCamera(this.mouse, this.camera);
    const intersects = this.raycaster.intersectObjects(this.scene.children, true);
    if (intersects.length > 0) {
      const hit = intersects[0].object;
      const node = this.findNodeForObject(this.objectTree(), hit);
      if (node) this.selectNode(node);
    }
  }

  private findNodeForObject(nodes: TreeNode[], target: THREE.Object3D): TreeNode | null {
    for (const node of nodes) {
      if (node.object === target || node.object === target.parent) return node;
      const found = this.findNodeForObject(node.children, target);
      if (found) return found;
    }
    return null;
  }

  resetCamera() { this.controls.reset(); }

  toggleWireframe() {
    this.scene.traverse(obj => {
      if (obj instanceof THREE.Mesh && obj.material instanceof THREE.MeshStandardMaterial) {
        obj.material.wireframe = !obj.material.wireframe;
      }
    });
  }

  private animate() {
    this.animFrameId = requestAnimationFrame(() => this.animate());
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }

  private onResize() {
    const canvas = this.canvasRef.nativeElement;
    const parent = canvas.parentElement;
    if (!parent) return;
    const w = parent.clientWidth, h = parent.clientHeight;
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }

  goBack() { this.router.navigate(['/items']); }

  ngOnDestroy() {
    cancelAnimationFrame(this.animFrameId);
    this.resizeObserver?.disconnect();
    this.renderer?.dispose();
  }
}