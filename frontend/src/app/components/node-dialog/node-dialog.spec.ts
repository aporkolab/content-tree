import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NodeDialog } from './node-dialog';
import { TreeNodeRequest } from '../../models/tree.model';

describe('NodeDialog', () => {
  let fixture: ComponentFixture<NodeDialog>;
  let el: HTMLElement;
  let saved: TreeNodeRequest[];
  let cancelled: number;

  async function setup(inputs: Record<string, unknown>): Promise<void> {
    fixture = TestBed.createComponent(NodeDialog);
    for (const [key, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(key, value);
    }
    saved = [];
    cancelled = 0;
    fixture.componentInstance.saved.subscribe((r) => saved.push(r));
    fixture.componentInstance.cancelled.subscribe(() => cancelled++);
    await fixture.whenStable();
    el = fixture.nativeElement;
  }

  function type(selector: string, value: string): void {
    const input = el.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  }

  it('does not emit and shows errors for blank fields', async () => {
    await setup({ mode: 'create', parentId: 1 });
    type('#name', '   ');

    await submit();

    expect(saved).toEqual([]);
    expect(el.textContent).toContain('Name is required');
    expect(el.textContent).toContain('Content is required');
  });

  it('emits a trimmed create request with the given parent', async () => {
    await setup({ mode: 'create', parentId: 7 });
    type('#name', '  Docs ');
    type('#content', ' Important ');

    await submit();

    expect(saved).toEqual([{ name: 'Docs', content: 'Important', parentId: 7 }]);
    expect(el.querySelector('h3')!.textContent).toContain('Create Node');
  });

  it('prefills edit mode and keeps the node parent', async () => {
    await setup({
      mode: 'edit',
      node: { id: 4, name: 'Old', content: 'Text', parentId: 2, children: [] },
    });
    expect(el.querySelector<HTMLInputElement>('#name')!.value).toBe('Old');

    type('#name', 'New');
    await submit();

    expect(saved).toEqual([{ name: 'New', content: 'Text', parentId: 2 }]);
    expect(el.querySelector('button[type=submit]')!.textContent).toContain('Save');
  });

  it('cancels on Escape and on overlay click, but not on clicks inside the dialog', async () => {
    await setup({ mode: 'create' });
    const overlay = el.querySelector<HTMLElement>('.dialog-overlay')!;

    el.querySelector<HTMLElement>('.dialog')!.click();
    expect(cancelled).toBe(0);

    overlay.click();
    el.querySelector('#name')!.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    );
    expect(cancelled).toBe(2);
  });
});
