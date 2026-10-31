import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TreeView } from './tree-view';
import { TreeNode } from '../../models/tree.model';

const API = 'http://localhost:8080/api/tree';

const sampleTree = (): TreeNode => ({
  id: 1,
  name: 'Root',
  content: 'root',
  parentId: null,
  children: [{ id: 2, name: 'Docs', content: 'docs', parentId: 1, children: [] }],
});

describe('TreeView', () => {
  let fixture: ComponentFixture<TreeView>;
  let component: TreeView;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TreeView);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  async function flushTree(tree: TreeNode | null, url = API): Promise<void> {
    http
      .expectOne((r) => r.url === url && r.method === 'GET')
      .flush(tree, tree ? {} : { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
  }

  it('loads the tree on start', async () => {
    await fixture.whenStable();
    expect(el.textContent).toContain('Loading tree');
    await flushTree(sampleTree());

    expect(component.loading()).toBe(false);
    expect(el.textContent).toContain('Docs');
    expect(el.textContent).not.toContain('New Root Node');
  });

  it('offers creating a root node when the tree is empty', async () => {
    await flushTree(null);

    expect(el.textContent).toContain('No tree yet');
    expect(el.textContent).toContain('New Root Node');
  });

  it('debounces search and only sends the latest query', async () => {
    await flushTree(sampleTree());
    vi.useFakeTimers();

    component.onSearch('d');
    component.onSearch('do');
    vi.advanceTimersByTime(300);

    const req = http.expectOne((r) => r.url === `${API}/search`);
    expect(req.request.params.get('query')).toBe('do');
    req.flush({ ...sampleTree(), isMatch: false });
    expect(component.tree()?.isMatch).toBe(false);
  });

  it('keeps the active search filter when reloading after a change', async () => {
    await flushTree(sampleTree());
    vi.useFakeTimers();
    component.onSearch('doc');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url === `${API}/search`).flush(sampleTree());

    component.openCreateDialog(1);
    component.onDialogSave({ name: 'New', content: 'c', parentId: 1 });
    http
      .expectOne({ method: 'POST', url: API })
      .flush({ id: 3, name: 'New', content: 'c', parentId: 1, children: [] });

    expect(http.expectOne((r) => r.url === `${API}/search`).request.params.get('query')).toBe(
      'doc',
    );
    expect(component.dialog().kind).toBe('none');
  });

  it('shows the backend problem detail when an update fails', async () => {
    await flushTree(sampleTree());

    component.openEditDialog(sampleTree().children[0]);
    component.onDialogSave({ name: 'X', content: 'Y', parentId: 1 });
    http
      .expectOne({ method: 'PUT', url: `${API}/2` })
      .flush(
        { title: 'Bad Request', detail: 'Cannot move node to its own descendant' },
        { status: 400, statusText: 'Bad Request' },
      );
    await flushTree(sampleTree());

    expect(component.errorMessage()).toBe('Cannot move node to its own descendant');
    expect(el.querySelector('.error-toast')?.textContent).toContain('Cannot move node');
  });

  it('clears the selection once the selected node is deleted', async () => {
    await flushTree(sampleTree());
    component.onNodeSelect(sampleTree().children[0]);
    expect(component.selectedNode()?.name).toBe('Docs');

    component.openDeleteConfirm(sampleTree().children[0]);
    component.onDeleteConfirm();
    http.expectOne({ method: 'DELETE', url: `${API}/2` }).flush(null);
    await flushTree({ ...sampleTree(), children: [] });

    expect(component.selectedNode()).toBeNull();
    expect(el.textContent).toContain('Select a node');
  });

  it('refreshes the selected node with the reloaded data', async () => {
    await flushTree(sampleTree());
    component.onNodeSelect(sampleTree().children[0]);

    component.reload();
    const renamed = sampleTree();
    renamed.children[0].name = 'Renamed';
    await flushTree(renamed);

    expect(component.selectedNode()?.name).toBe('Renamed');
  });

  it('ignores drops onto the current parent and moves otherwise', async () => {
    await flushTree(sampleTree());
    const docs = sampleTree().children[0];
    const drop = (target: TreeNode) =>
      component.onNodeDrop({ item: { data: docs }, container: { data: target } } as never);

    drop(sampleTree());
    http.expectNone(`${API}/move`);

    drop({ id: 9, name: 'Other', content: '', parentId: 1, children: [] });
    expect(http.expectOne(`${API}/move`).request.body).toEqual({ nodeId: 2, newParentId: 9 });
  });
});
