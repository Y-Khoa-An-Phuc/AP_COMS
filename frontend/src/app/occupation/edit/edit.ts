import { Component, OnInit, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Occupation, OccupationRequest } from '../../model/occupation';
import { OccupationService } from '../occupation.service';

@Component({
  selector: 'app-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './edit.html',
  styleUrls: ['./edit.css'],
})
export class EditOccupation implements OnInit {
  occupationForm: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private occupationService: OccupationService,
    private cdr: ChangeDetectorRef,
    public dialogRef: MatDialogRef<EditOccupation>,
    @Inject(MAT_DIALOG_DATA) public data: Occupation
  ) {
    this.occupationForm = this.fb.group({
      occCode: [{ value: '', disabled: true }],
      occName: [{ value: '', disabled: true }],
      description: ['', Validators.maxLength(500)]
    });
  }

  ngOnInit() {
    // Populate form with existing data
    this.occupationForm.patchValue({
      occCode: this.data.occCode,
      occName: this.data.occName,
      description: this.data.description || ''
    });
  }

  onSubmit() {
    if (this.occupationForm.invalid) {
      Object.keys(this.occupationForm.controls).forEach(key => {
        this.occupationForm.get(key)?.markAsTouched();
      });
      return;
    }

    const requestBody: OccupationRequest = {
      name: this.data.occName, // Name is readonly, send original
      description: this.occupationForm.get('description')?.value || ''
    };

    this.isLoading = true;
    this.errorMessage = '';

    this.occupationService.update(this.data.occCode, requestBody).subscribe({
      next: (response) => {
        console.log('Occupation updated successfully:', response);

        // Close dialog with updated data
        this.dialogRef.close({
          occCode: response.occCode,
          occName: response.occName,
          description: response.description
        });

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('Error updating occupation:', error);
        this.isLoading = false;

        // ApiClient returns normalized error with status and message
        if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
        } else if (error.status === 403) {
          this.errorMessage = 'Bạn không có quyền thực hiện thao tác này.';
        } else if (error.status === 400) {
          this.errorMessage = error.message || 'Dữ liệu không hợp lệ.';
        } else if (error.status === 404) {
          this.errorMessage = 'Nghề nghiệp không tồn tại.';
        } else if (error.status >= 500) {
          this.errorMessage = error.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
        } else {
          this.errorMessage = error.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  onCancel() {
    this.dialogRef.close();
  }
}