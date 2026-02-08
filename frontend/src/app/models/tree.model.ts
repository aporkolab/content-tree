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
