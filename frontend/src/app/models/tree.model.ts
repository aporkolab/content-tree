export interface TreeNode {
  id: number;
  name: string;
  content: string;
  parentId: number | null;
  children: TreeNode[];
  isMatch?: boolean | null;
}

export interface TreeNodeRequest {
  name: string;
  content: string;
  parentId: number | null;
}

export interface MoveNodeRequest {
  nodeId: number;
  newParentId: number | null;
}

/** RFC 9457 error body returned by the backend. */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

/** Limits enforced by the backend (TreeNodeRequest). */
export const NAME_MAX_LENGTH = 255;
export const CONTENT_MAX_LENGTH = 10_000;
