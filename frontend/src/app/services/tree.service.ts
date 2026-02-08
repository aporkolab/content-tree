import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TreeNode, TreeNodeRequest } from '../models/tree.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TreeService {
  private readonly apiUrl = `${environment.apiUrl}/tree`;
  private readonly http = inject(HttpClient);

  getTree(): Observable<TreeNode> {
    return this.http.get<TreeNode>(this.apiUrl);
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
    return this.http.post<void>(`${this.apiUrl}/move`, { nodeId, newParentId });
  }

  searchTree(query: string): Observable<TreeNode> {
    return this.http.get<TreeNode>(`${this.apiUrl}/search`, { params: { query } });
  }
}
