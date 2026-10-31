import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MoveNodeRequest, ProblemDetail, TreeNode, TreeNodeRequest } from '../models/tree.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TreeService {
  private readonly apiUrl = `${environment.apiUrl}/tree`;
  private readonly http = inject(HttpClient);

  /** Emits null when the tree is empty (204 No Content). */
  getTree(): Observable<TreeNode | null> {
    return this.http.get<TreeNode | null>(this.apiUrl);
  }

  getNode(id: number): Observable<TreeNode> {
    return this.http.get<TreeNode>(`${this.apiUrl}/${id}`);
  }

  createNode(request: TreeNodeRequest): Observable<TreeNode> {
    return this.http.post<TreeNode>(this.apiUrl, request);
  }

  updateNode(id: number, request: TreeNodeRequest): Observable<TreeNode> {
    return this.http.put<TreeNode>(`${this.apiUrl}/${id}`, request);
  }

  deleteNode(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  moveNode(nodeId: number, newParentId: number | null): Observable<void> {
    const body: MoveNodeRequest = { nodeId, newParentId };
    return this.http.post<void>(`${this.apiUrl}/move`, body);
  }

  /** Emits null when the tree is empty (204 No Content). */
  searchTree(query: string): Observable<TreeNode | null> {
    return this.http.get<TreeNode | null>(`${this.apiUrl}/search`, { params: { query } });
  }
}

/** Extracts a user facing message from an HTTP error. */
export function toErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const body: unknown = err.error;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    const detail = (body as ProblemDetail | null)?.detail;
    if (typeof detail === 'string' && detail.trim()) {
      return detail;
    }
  }
  return fallback;
}

/** Depth-first lookup of a node by id. */
export function findNodeById(node: TreeNode | null, id: number | null): TreeNode | null {
  if (!node || id === null) return null;
  if (node.id === id) return node;
  for (const child of node.children ?? []) {
    const found = findNodeById(child, id);
    if (found) return found;
  }
  return null;
}
