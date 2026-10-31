import {
  Component,
  ElementRef,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  CONTENT_MAX_LENGTH,
  NAME_MAX_LENGTH,
  TreeNode,
  TreeNodeRequest,
} from '../../models/tree.model';

/** Like Validators.required, but also rejects whitespace-only input (mirrors @NotBlank). */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { required: true };
}

@Component({
  selector: 'app-node-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './node-dialog.html',
  styleUrl: './node-dialog.scss',
})
export class NodeDialog implements OnInit {
  readonly mode = input<'create' | 'edit'>('create');
  readonly parentId = input<number | null>(null);
  readonly node = input<TreeNode | null>(null);

  readonly saved = output<TreeNodeRequest>();
  readonly cancelled = output<void>();

  readonly nameMaxLength = NAME_MAX_LENGTH;
  readonly contentMaxLength = CONTENT_MAX_LENGTH;

  readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [notBlank, Validators.maxLength(NAME_MAX_LENGTH)]],
    content: ['', [notBlank, Validators.maxLength(CONTENT_MAX_LENGTH)]],
  });

  readonly submitted = signal(false);
  readonly title = computed(() => (this.mode() === 'create' ? 'Create Node' : 'Edit Node'));

  private readonly nameInput = viewChild.required<ElementRef<HTMLInputElement>>('nameInput');

  constructor() {
    afterNextRender(() => this.nameInput().nativeElement.focus());
  }

  ngOnInit(): void {
    const node = this.node();
    if (this.mode() === 'edit' && node) {
      this.form.setValue({ name: node.name, content: node.content });
    }
  }

  showError(control: 'name' | 'content'): boolean {
    return this.submitted() && this.form.controls[control].invalid;
  }

  onSubmit(): void {
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }

    const { name, content } = this.form.getRawValue();
    this.saved.emit({
      name: name.trim(),
      content: content.trim(),
      parentId: this.mode() === 'create' ? this.parentId() : (this.node()?.parentId ?? null),
    });
  }

  onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelled.emit();
    }
  }
}
