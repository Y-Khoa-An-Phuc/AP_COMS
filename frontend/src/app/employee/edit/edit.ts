import { Component, OnInit, Optional, ChangeDetectionStrategy, signal, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
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
import { AddEmployeeService } from '../add/add.services';
import { Employee } from '../../model/employee';
import { Branch } from '../../model/branch';

@Component({
  selector: 'app-edit-employee',
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
  templateUrl: './edit.html',
  styleUrls: ['./edit.css'],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmployee implements OnInit {
  employeeForm: FormGroup;
  isDialog: boolean = false;
  occupations = signal<Occupation[]>([]);

  constructor(
    private fb: FormBuilder,
    private addEmployeeService: AddEmployeeService,
    @Optional() private dialogRef: MatDialogRef<EditEmployee>,
    @Inject(MAT_DIALOG_DATA) public data: Employee
  ) {
    this.isDialog = !!this.dialogRef;
    this.employeeForm = this.fb.group({
      employeeId: [{ value: '', disabled: true }], // Read-only
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
    // Load occupations
    this.addEmployeeService.loadOccupations().subscribe({
      next: (data) => {
        this.occupations.set(data);
      },
      error: (err) => {
        console.error('Failed to load occupations:', err);
      }
    });

    // Pre-populate form with employee data
    if (this.data) {
      const workLoc = this.data.branch as unknown as Branch;
      this.employeeForm.patchValue({
        employeeId: this.data.empId,
        email: this.data.email,
        fullName: this.data.fullName,
        lastName: this.data.lastName,
        middleName: this.data.middleName,
        firstName: this.data.firstName,
        hireDt: this.data.hireDt,
        terminationDt: this.data.terminationDt,
        citizenIdCard: this.data.citizenIdCard,
        passport: this.data.passport,
        contractId: this.data.contractId,
        occupation: this.data.occupation?.occCode || '',
        organization: this.data.organization?.orgLabel || '',
        workLocation: workLoc?.branchName || '',
        phone: this.data.phone,
        supvEmployee: this.data.supvEmployee?.empId || '',
        workStatus: this.data.workStatus
      });
    }
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
      console.log('Updated Employee Data:', formData);

      // If opened as dialog, close with data
      if (this.isDialog && this.dialogRef) {
        this.dialogRef.close(formData);
      }
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.employeeForm.controls).forEach(key => {
        this.employeeForm.get(key)?.markAsTouched();
      });
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
