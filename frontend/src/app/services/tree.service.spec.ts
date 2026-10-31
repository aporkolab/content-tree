import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TreeService, findNodeById, toErrorMessage } from './tree.service';
import { TreeNode } from '../models/tree.model';

const API = 'http://localhost:8080/api/tree';

describe('TreeService', () => {
  let service: TreeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TreeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps 204 No Content on GET /tree to null', () => {
    let result: TreeNode | null | undefined;
    service.getTree().subscribe((t) => (result = t));

    http.expectOne(API).flush(null, { status: 204, statusText: 'No Content' });

    expect(result).toBeNull();
  });

  it('sends the search query as a parameter', () => {
    service.searchTree('a b').subscribe();

    const req = http.expectOne((r) => r.url === `${API}/search`);
    expect(req.request.params.get('query')).toBe('a b');
    req.flush(null);
  });

  it('posts move requests with nodeId and newParentId', () => {
    service.moveNode(2, 5).subscribe();

    const req = http.expectOne(`${API}/move`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nodeId: 2, newParentId: 5 });
    req.flush(null);
  });

  it('uses PUT for updates and DELETE for deletes', () => {
    service.updateNode(3, { name: 'n', content: 'c', parentId: 1 }).subscribe();
    service.deleteNode(3).subscribe();

    expect(http.expectOne({ method: 'PUT', url: `${API}/3` }).request.body).toEqual({
      name: 'n',
      content: 'c',
      parentId: 1,
    });
    http.expectOne({ method: 'DELETE', url: `${API}/3` }).flush(null);
  });
});

describe('toErrorMessage', () => {
  const error = (body: unknown) => new HttpErrorResponse({ error: body, status: 400 });

  it('prefers the problem detail message', () => {
    expect(
      toErrorMessage(error({ title: 'Bad Request', detail: 'Root node already exists' }), 'x'),
    ).toBe('Root node already exists');
  });

  it('accepts plain string bodies', () => {
    expect(toErrorMessage(error('Something broke'), 'x')).toBe('Something broke');
  });

  it('falls back for objects without detail, empty bodies and non-HTTP errors', () => {
    expect(toErrorMessage(error({ errors: { name: 'Name is required' } }), 'fallback')).toBe(
      'fallback',
    );
    expect(toErrorMessage(error(null), 'fallback')).toBe('fallback');
    expect(toErrorMessage(new Error('boom'), 'fallback')).toBe('fallback');
  });
});

describe('findNodeById', () => {
  const tree: TreeNode = {
    id: 1,
    name: 'Root',
    content: '',
    parentId: null,
    children: [
      {
        id: 2,
        name: 'A',
        content: '',
        parentId: 1,
        children: [{ id: 3, name: 'A1', content: '', parentId: 2, children: [] }],
      },
    ],
  };

  it('finds nested nodes', () => {
    expect(findNodeById(tree, 3)?.name).toBe('A1');
  });

  it('returns null for unknown ids, null ids and null trees', () => {
    expect(findNodeById(tree, 99)).toBeNull();
    expect(findNodeById(tree, null)).toBeNull();
    expect(findNodeById(null, 1)).toBeNull();
  });
});
