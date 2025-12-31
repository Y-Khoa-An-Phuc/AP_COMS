import { Component, OnInit, Optional, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { provideNativeDateAdapter } from '@angular/material/core';
import { Occupation } from '../../model/occupation';
import { AddEmployeeService } from './add.services';

@Component({
  selector: 'app-add-employee',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './add.html',
  styleUrls: ['./add.css'],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddEmployee implements OnInit {
  employeeForm: FormGroup;
  isDialog: boolean = false;
  occupations = signal<Occupation[]>([]);
  workStatusOptions = ['Đang làm việc', 'Nghỉ việc', 'Nghỉ thai sản'];

  constructor(
    private fb: FormBuilder,
    private addEmployeeService: AddEmployeeService,
    @Optional() private dialogRef: MatDialogRef<AddEmployee>
  ) {
    this.isDialog = !!this.dialogRef;
    this.employeeForm = this.fb.group({
      email: ['', Validators.email],
      fullName: [''],
      lastName: ['', Validators.required],
      middleName: [''],
      firstName: ['', Validators.required],
      hireDt: ['', Validators.required],
      terminationDt: [''],
      citizenIdCard: [''],
      passport: [''],
      contractId: [''],
      occupation: [''],
      organization: [''],
      workLocation: [''],
      phone: [''],
      supvEmployee: [''],
      workStatus: ['']
    }, { validators: this.identityValidator });
  }

  ngOnInit() {
    this.addEmployeeService.loadOccupations().subscribe({
      next: (data) => {
        this.occupations.set(data);
      },
      error: (err) => {
        console.error('Failed to load occupations:', err);
      }
    });
  }

  // Custom validator to ensure either citizenIdCard or passport is provided
  identityValidator(control: AbstractControl): ValidationErrors | null {
    const citizenIdCard = control.get('citizenIdCard')?.value;
    const passport = control.get('passport')?.value;

    if (!citizenIdCard && !passport) {
      return { identityRequired: true };
    }
    return null;
  }

  onSubmit() {
    if (this.employeeForm.valid) {
      const formData = this.employeeForm.getRawValue();
      console.log('Form Data:', formData);

      // If opened as dialog, close with data
      if (this.isDialog && this.dialogRef) {
        this.dialogRef.close(formData);
      }
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.employeeForm.controls).forEach(key => {
        this.employeeForm.get(key)?.markAsTouched();
      });
      // Also mark the form itself to trigger form-level validators
      this.employeeForm.markAsTouched();
    }
  }

  onCancel() {
    if (this.isDialog && this.dialogRef) {
      this.dialogRef.close();
    } else {
      this.employeeForm.reset();
    }
  }
}
