import { Component, OnInit, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AddEmployee } from '../add/add';
import { EditEmployee } from '../edit/edit';
import { Employee } from '../../model/employee';
import { Branch } from '../../model/branch';
import { Occupation } from '../../model/occupation';
import { Organization } from '../../model/org';

@Component({
  selector: 'app-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule
  ],
  templateUrl: './view.html',
  styleUrls: ['./view.css'],
})
export class ViewEmployee implements OnInit {
  displayedColumns: string[] = ['edit', 'delete', 'select', 'empId', 'lastName', 'middleName', 'firstName', 'fullName', 'email', 'phone', 'hireDt', 'terminationDt', 'citizenIdCard', 'passport', 'contractId', 'occupation', 'organization', 'workLocation', 'supvEmployee', 'workStatus', 'createTimestamp', 'createUser', 'lastUpdateTimestamp'];
  selectedEmployees: Set<string> = new Set();

  constructor(
    private dialog: MatDialog,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  employees: Employee[] = [
    {
      empId: 'AP001',
      firstName: 'A',
      lastName: 'Nguyễn',
      middleName: '',
      fullName: 'Nguyễn A',
      email: 'abc@opg.c',
      hireDt: '2025-01-15',
      terminationDt: null,
      citizenIdCard: '001234567890',
      passport: '',
      contractId: 'CT001',
      occupation: { occCode: 'BS', occName: 'Bác sĩ' },
      organization: { orgId: 1, orgLabel: 'An Phúc', orgDescr: 'An Phúc Medical' },
      branch: {
        branchId: 1,
        branchName: 'An Phúc Biên Hòa',
        address: '123 Biên Hòa',
        city: 'Biên Hòa',
        province: 'Đồng Nai',
        phone: '0251234567'
      },
      phone: '0901234567',
      supvEmployee: null,
      createUser: 'SuperAdmin',
      createTimestamp: '2025-05-10T12:00:01',
      lastUpdateUser: 'SuperAdmin',
      lastUpdateTimestamp: '2025-05-10T12:00:01',
      workStatus: 'Đang hoạt động'
    },
    {
      empId: 'AP002',
      firstName: 'B',
      lastName: 'Trần',
      middleName: 'Văn',
      fullName: 'Trần Văn B',
      email: 'test@a.com',
      hireDt: '2025-02-01',
      terminationDt: null,
      citizenIdCard: '',
      passport: 'P123456',
      contractId: 'CT002',
      occupation: { occCode: 'KT', occName: 'Kế Toán' },
      organization: { orgId: 1, orgLabel: 'An Phúc', orgDescr: 'An Phúc Medical' },
      branch: {
        branchId: 2,
        branchName: 'An Phúc Long Thành',
        address: '456 Long Thành',
        city: 'Long Thành',
        province: 'Đồng Nai',
        phone: '0251234568'
      },
      phone: '0907654321',
      supvEmployee: null,
      createUser: 'SuperAdmin',
      createTimestamp: '2025-05-10T12:00:01',
      lastUpdateUser: 'SuperAdmin',
      lastUpdateTimestamp: '2025-05-10T12:00:01',
      workStatus: 'Đang hoạt động'
    }
  ];

  dataSource = new MatTableDataSource<Employee>();
  searchText: string = '';

  ngOnInit() {
    this.dataSource.data = this.employees;
  }

  onSearch() {
    this.filterEmployees();
  }

  filterEmployees() {
    const filtered = this.employees.filter(emp => {
      const workLoc = emp.branch as unknown as Branch;
      const matchesSearch = !this.searchText ||
        emp.fullName.toLowerCase().includes(this.searchText.toLowerCase()) ||
        emp.empId.toLowerCase().includes(this.searchText.toLowerCase()) ||
        emp.email.toLowerCase().includes(this.searchText.toLowerCase()) ||
        (emp.phone || '').toLowerCase().includes(this.searchText.toLowerCase()) ||
        (emp.occupation?.occName || '').toLowerCase().includes(this.searchText.toLowerCase()) ||
        (emp.organization?.orgLabel || '').toLowerCase().includes(this.searchText.toLowerCase()) ||
        (workLoc?.branchName || '').toLowerCase().includes(this.searchText.toLowerCase());

      return matchesSearch;
    });
    // Update dataSource
    this.dataSource.data = filtered;
  }

  onExportExcel() {
    console.log('Export to Excel');
    // Implement Excel export logic
  }

  onAddEmployee() {
    const dialogRef = this.dialog.open(AddEmployee, {
      width: '800px',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Add the new employee to the list
        const newEmployee: Employee = {
          empId: result.employeeId || `AP${(this.employees.length + 1).toString().padStart(3, '0')}`,
          firstName: result.firstName || '',
          lastName: result.lastName || '',
          middleName: result.middleName || '',
          fullName: result.fullName || '',
          email: result.email || '',
          hireDt: result.hireDt || new Date().toISOString(),
          terminationDt: result.terminationDt || null,
          citizenIdCard: result.citizenIdCard || '',
          passport: result.passport || '',
          contractId: result.contractId || '',
          occupation: result.occupation || { occCode: '', occName: '' },
          organization: result.organization || { orgId: 1, orgLabel: 'An Phúc', orgDescr: '' },
          branch: result.branch || { branchId: 0, branchName: '', address: '', city: '', province: '', phone: '' },
          phone: result.phone || '',
          supvEmployee: null,
          createUser: 'CurrentUser',
          createTimestamp: new Date().toISOString(),
          lastUpdateUser: 'CurrentUser',
          lastUpdateTimestamp: new Date().toISOString(),
          workStatus: result.workStatus || 'Đang hoạt động'
        };
        this.employees.push(newEmployee);
        this.filterEmployees();
        console.log('New employee added:', newEmployee);
      }
    });
  }

  onEmployeeClick(employee: Employee) {
    console.log('Employee clicked:', employee);
    // Navigate to employee detail page
  }

  onEditEmployee(employee: Employee) {
    const dialogRef = this.dialog.open(EditEmployee, {
      width: '800px',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: true,
      data: employee
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Find and update the employee in the array
        const index = this.employees.findIndex(emp => emp.empId === employee.empId);
        if (index !== -1) {
          // Update the employee with new data
          this.employees[index] = {
            ...this.employees[index],
            firstName: result.firstName || this.employees[index].firstName,
            lastName: result.lastName || this.employees[index].lastName,
            middleName: result.middleName || this.employees[index].middleName,
            fullName: result.fullName || this.employees[index].fullName,
            email: result.email || this.employees[index].email,
            hireDt: result.hireDt || this.employees[index].hireDt,
            terminationDt: result.terminationDt || this.employees[index].terminationDt,
            citizenIdCard: result.citizenIdCard || this.employees[index].citizenIdCard,
            passport: result.passport || this.employees[index].passport,
            contractId: result.contractId || this.employees[index].contractId,
            phone: result.phone || this.employees[index].phone,
            workStatus: result.workStatus || this.employees[index].workStatus,
            lastUpdateUser: 'CurrentUser',
            lastUpdateTimestamp: new Date().toISOString()
          };
          // Re-apply the current search filter
          this.filterEmployees();
          console.log('Employee updated:', this.employees[index]);
        }
      }
    });
  }

  onDeleteEmployee(employee: Employee) {
    // Open confirmation dialog
    const dialogRef = this.dialog.open(ConfirmDeleteDialog, {
      width: '400px',
      data: {
        employeeName: employee.fullName,
        employeeCode: employee.empId
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        // User confirmed deletion
        // Remove from selection first
        this.selectedEmployees.delete(employee.empId);
        // Remove from main array - create new reference
        this.employees = this.employees.filter(emp => emp.empId !== employee.empId);
        // Re-apply the current search filter
        this.filterEmployees();
        // Manually trigger change detection
        this.cdr.detectChanges();
        console.log('Employee deleted:', this.dataSource.data);
      }
    });
  }

  // Selection methods
  isSelected(employee: Employee): boolean {
    return this.selectedEmployees.has(employee.empId);
  }

  onSelectEmployee(employee: Employee, checked: boolean) {
    if (checked) {
      this.selectedEmployees.add(employee.empId);
    } else {
      this.selectedEmployees.delete(employee.empId);
    }
  }

  isAllSelected(): boolean {
    const currentData = this.dataSource.data;
    if (currentData.length === 0) return false;
    return currentData.every(emp => this.selectedEmployees.has(emp.empId));
  }

  isSomeSelected(): boolean {
    const currentData = this.dataSource.data;
    if (currentData.length === 0) return false;
    const selectedCount = currentData.filter(emp => this.selectedEmployees.has(emp.empId)).length;
    return selectedCount > 0 && selectedCount < currentData.length;
  }

  onSelectAll(checked: boolean) {
    if (checked) {
      this.dataSource.data.forEach(emp => this.selectedEmployees.add(emp.empId));
    } else {
      this.selectedEmployees.clear();
    }
  }
}

// Confirmation Delete Dialog Component
@Component({
  selector: 'confirm-delete-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Xác nhận xóa</h2>
    <mat-dialog-content>
      <p>Bạn có chắc chắn muốn xóa nhân viên này?</p>
      <p class="employee-info"><strong>{{ data.employeeName }}</strong> ({{ data.employeeCode }})</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Không</button>
      <button mat-raised-button color="warn" (click)="onConfirm()">Có, xóa</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      padding: 20px 24px;
    }
    .employee-info {
      margin-top: 16px;
      padding: 12px;
      background-color: #f5f5f5;
      border-radius: 4px;
    }
    mat-dialog-actions {
      padding: 16px 24px;
      gap: 12px;
    }
  `]
})
export class ConfirmDeleteDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { employeeName: string; employeeCode: string }
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
