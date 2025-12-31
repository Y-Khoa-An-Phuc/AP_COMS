import { Component, OnInit, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { OverlayModule } from '@angular/cdk/overlay';
import { Occupation } from '../../model/occupation';
import { AddOccupation } from '../add/add';
import { EditOccupation } from '../edit/edit';
import { OccupationService } from '../occupation.service';
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatMenuModule,
    MatSelectModule,
    OverlayModule
  ],
  templateUrl: './view.html',
  styleUrls: ['./view.css'],
})
export class ViewOccupation implements OnInit {
  displayedColumns: string[] = ['edit', 'delete', 'select', 'occCode', 'occName', 'description'];
  selectedOccupations: Set<string> = new Set();
  occupations: Occupation[] = [];
  dataSource = new MatTableDataSource<Occupation>();
  isLoading = false;
  errorMessage = '';

  // Dynamic column configuration for Excel export
  // Maps column IDs to their Vietnamese headers
  private columnHeaders: { [key: string]: string } = {
    'occCode': 'Mã Nghề Nghiệp',
    'occName': 'Tên Nghề Nghiệp',
    'description': 'Mô Tả'
  };

  // Filter panel state
  isFilterPanelOpen = false;
  availableFilterColumns: Array<{
    field: string;
    label: string;
    type: string;
    operators: string[];
    valuesEndpoint?: string;
  }> = [];
  filterConditions: Array<{
    field: string;
    label: string;
    type: string;
    operator: string;
    selectedValues?: string[];
    textValue?: string;
    options?: string[];
  }> = [];

  // Dummy data for filter options (until backend endpoint is ready)
  private dummyOccupationNames = ['Kỹ sư phần mềm', 'Bác sĩ', 'Giáo viên', 'Kế toán', 'Luật sư'];

  constructor(
    private dialog: MatDialog,
    private occupationService: OccupationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadOccupations();
    this.loadFilterMetadata();
  }

  loadFilterMetadata() {
    this.occupationService.getFilterMetadata().subscribe({
      next: (response) => {
        console.log('Filter metadata response:', response);

        // Populate availableFilterColumns from the response
        if (response?.data && Array.isArray(response.data)) {
          this.availableFilterColumns = response.data.map((item: any) => ({
            field: item.field,
            label: item.label,
            type: item.type,
            operators: item.operators || [],
            valuesEndpoint: item.valuesEndpoint
          }));
        }
      },
      error: (error) => {
        console.error('Error fetching filter metadata:', error);
      }
    });
  }

  loadOccupations() {
    this.isLoading = true;
    this.errorMessage = '';

    this.occupationService.list().subscribe({
      next: (response) => {
        console.log('Occupations loaded:', response);

        this.occupations = response;
        this.dataSource.data = this.occupations;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('Error loading occupations:', error);
        this.isLoading = false;

        // ApiClient returns normalized error with status and message
        // 401 errors are automatically redirected to login by ApiClient
        if (error.status === 0) {
          this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
        } else if (error.status === 403) {
          this.errorMessage = 'Bạn không có quyền xem danh sách nghề nghiệp.';
        } else if (error.status >= 500) {
          this.errorMessage = error.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
        } else {
          this.errorMessage = error.message || 'Đã xảy ra lỗi khi tải danh sách nghề nghiệp.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  onExportExcel() {
    // Check if any occupations are selected
    if (this.selectedOccupations.size === 0) {
      // Show dialog asking user to select records
      this.dialog.open(NoSelectionDialog, {
        width: '400px',
        data: {
          message: 'Vui lòng chọn ít nhất một nghề nghiệp để xuất Excel.'
        }
      });
      return;
    }

    // Export only selected occupations
    const occupationsToExport = this.occupations.filter(occ =>
      this.selectedOccupations.has(occ.occCode)
    );

    // Get data columns (exclude action columns like edit, delete, select)
    const dataColumns = this.displayedColumns.filter(col =>
      col !== 'edit' && col !== 'delete' && col !== 'select'
    );

    // Dynamically build headers from columnHeaders mapping
    const headers = dataColumns.map(col => this.columnHeaders[col] || col);

    // Build data rows dynamically based on dataColumns
    const data = occupationsToExport.map(occ => {
      const row: any = {};
      dataColumns.forEach(col => {
        const header = this.columnHeaders[col] || col;
        // Map occupation property to column
        const value = (occ as any)[col];
        row[header] = value || '';
      });
      return row;
    });

    // Create worksheet
    const worksheet = XLSX.utils.json_to_sheet(data, { header: headers });

    // Set column widths for better readability
    const columnWidths = dataColumns.map(col => {
      switch(col) {
        case 'occCode': return { wch: 15 };
        case 'occName': return { wch: 30 };
        case 'description': return { wch: 50 };
        default: return { wch: 20 };
      }
    });
    worksheet['!cols'] = columnWidths;

    // Create workbook
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Nghề Nghiệp');

    // Generate filename with timestamp
    const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    const filename = `Danh_Sach_Nghe_Nghiep_${timestamp}.xlsx`;

    // Export to file
    XLSX.writeFile(workbook, filename);

    console.log(`Exported ${occupationsToExport.length} occupations to ${filename}`);
  }

  onViewHistory() {
    // Check if any occupations are selected
    if (this.selectedOccupations.size === 0) {
      // Show dialog asking user to select records
      this.dialog.open(NoSelectionDialog, {
        width: '400px',
        data: {
          message: 'Vui lòng chọn ít nhất một nghề nghiệp để xem lịch sử.'
        }
      });
      return;
    }

    console.log('View history for selected occupations:', this.selectedOccupations);
    // TODO: Implement history view functionality
  }

  onAddOccupation() {
    const dialogRef = this.dialog.open(AddOccupation, {
      width: '600px',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Reload occupations from the server to get the latest data
        this.loadOccupations();
      }
    });
  }

  onEditOccupation(occupation: Occupation) {
    // Check if multiple occupations are selected
    if (this.selectedOccupations.size > 1) {
      this.errorMessage = 'Bạn chỉ có thể chỉnh sửa một nghề nghiệp tại một thời điểm. Vui lòng bỏ chọn các nghề nghiệp khác.';
      this.cdr.detectChanges();
      setTimeout(() => {
        this.errorMessage = '';
        this.cdr.detectChanges();
      }, 3000);
      return;
    }

    const dialogRef = this.dialog.open(EditOccupation, {
      width: '600px',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: true,
      data: occupation
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Update the occupation in the local array
        const index = this.occupations.findIndex(occ => occ.occCode === result.occCode);
        if (index !== -1) {
          this.occupations[index] = result;
          // Update the table dataSource
          this.dataSource.data = this.occupations;
          this.cdr.detectChanges();
        }
      }
    });
  }

  onDeleteOccupation(occupation: Occupation) {
    // Check if multiple occupations are selected
    if (this.selectedOccupations.size > 1) {
      this.errorMessage = 'Bạn chỉ có thể xóa một nghề nghiệp tại một thời điểm. Vui lòng bỏ chọn các nghề nghiệp khác.';
      this.cdr.detectChanges();
      setTimeout(() => {
        this.errorMessage = '';
        this.cdr.detectChanges();
      }, 3000);
      return;
    }

    // Open confirmation dialog
    const dialogRef = this.dialog.open(ConfirmDeleteOccupationDialog, {
      width: '400px',
      data: {
        occupationName: occupation.occName,
        occupationCode: occupation.occCode
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        // User confirmed deletion - call backend API
        this.isLoading = true;
        this.errorMessage = '';

        this.occupationService.delete(occupation.occCode).subscribe({
          next: () => {
            console.log('Occupation deleted successfully:', occupation.occCode);

            // Remove from selection first
            this.selectedOccupations.delete(occupation.occCode);
            // Remove from main array - create new reference
            this.occupations = this.occupations.filter(occ => occ.occCode !== occupation.occCode);
            // Update the table dataSource
            this.dataSource.data = this.occupations;

            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: (error: any) => {
            console.error('Error deleting occupation:', error);
            this.isLoading = false;

            // ApiClient returns normalized error with status and message
            if (error.status === 0) {
              this.errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
            } else if (error.status === 403) {
              this.errorMessage = 'Bạn không có quyền xóa nghề nghiệp này.';
            } else if (error.status === 404) {
              this.errorMessage = 'Nghề nghiệp không tồn tại.';
            } else if (error.status === 409) {
              this.errorMessage = error.message || 'Không thể xóa nghề nghiệp đang được sử dụng.';
            } else if (error.status >= 500) {
              this.errorMessage = error.message || 'Lỗi máy chủ. Vui lòng thử lại sau.';
            } else {
              this.errorMessage = error.message || 'Đã xảy ra lỗi khi xóa nghề nghiệp.';
            }

            this.cdr.detectChanges();
          }
        });
      }
    });
  }

  // Selection methods
  isSelected(occupation: Occupation): boolean {
    return this.selectedOccupations.has(occupation.occCode);
  }

  onSelectOccupation(occupation: Occupation, checked: boolean) {
    if (checked) {
      this.selectedOccupations.add(occupation.occCode);
    } else {
      this.selectedOccupations.delete(occupation.occCode);
    }
  }

  isAllSelected(): boolean {
    const currentData = this.dataSource.data;
    if (currentData.length === 0) return false;
    return currentData.every(occ => this.selectedOccupations.has(occ.occCode));
  }

  isSomeSelected(): boolean {
    const currentData = this.dataSource.data;
    if (currentData.length === 0) return false;
    const selectedCount = currentData.filter(occ => this.selectedOccupations.has(occ.occCode)).length;
    return selectedCount > 0 && selectedCount < currentData.length;
  }

  onSelectAll(checked: boolean) {
    if (checked) {
      this.dataSource.data.forEach(occ => this.selectedOccupations.add(occ.occCode));
    } else {
      this.selectedOccupations.clear();
    }
  }

  // Filter panel methods
  toggleFilterPanel() {
    this.isFilterPanelOpen = !this.isFilterPanelOpen;
  }

  closeFilterPanel() {
    this.isFilterPanelOpen = false;
  }

  onAddCondition(fieldKey: string) {
    // Clear all existing conditions (only allow one at a time)
    this.filterConditions = [];

    // Find the column configuration
    const column = this.availableFilterColumns.find(c => c.field === fieldKey);
    if (!column) {
      return;
    }

    // Get the first operator (we'll use the first one by default)
    const operator = column.operators[0];

    // Determine if we need dropdown or text input based on operator
    if (operator === 'IN') {
      // For IN operator, fetch values from valuesEndpoint if available
      if (column.valuesEndpoint) {
        // Fetch values from API
        this.occupationService.getFilterValues(column.valuesEndpoint).subscribe({
          next: (response) => {
            console.log('Filter values response:', response);

            // Extract options from response (assuming response.data is an array)
            const options = response?.data || [];

            this.filterConditions.push({
              field: fieldKey,
              label: column.label,
              type: column.type,
              operator: operator,
              selectedValues: [],
              options: options
            });
          },
          error: (error) => {
            console.error('Error fetching filter values:', error);
            // Still add the condition but with empty options
            this.filterConditions.push({
              field: fieldKey,
              label: column.label,
              type: column.type,
              operator: operator,
              selectedValues: [],
              options: []
            });
          }
        });
      } else {
        // No valuesEndpoint, use dummy data as fallback
        let options: string[] = [];
        if (fieldKey === 'name') {
          options = [...this.dummyOccupationNames];
        }

        this.filterConditions.push({
          field: fieldKey,
          label: column.label,
          type: column.type,
          operator: operator,
          selectedValues: [],
          options: options
        });
      }
    } else if (operator === 'CONTAINS') {
      // For CONTAINS operator, use text input
      this.filterConditions.push({
        field: fieldKey,
        label: column.label,
        type: column.type,
        operator: operator,
        textValue: ''
      });
    }
  }

  removeCondition(index: number) {
    this.filterConditions.splice(index, 1);
  }

  toggleSelectAll(condition: any) {
    // Only for conditions with dropdown (IN operator)
    if (!condition.options || !condition.selectedValues) {
      return;
    }

    // Check if all options are currently selected
    const allSelected = condition.options.length === condition.selectedValues.filter((v: string) => v !== '__SELECT_ALL__').length;

    if (allSelected) {
      // Deselect all
      condition.selectedValues = [];
    } else {
      // Select all options (excluding the __SELECT_ALL__ marker)
      condition.selectedValues = [...condition.options];
    }
  }

  applyFilters() {
    // If no conditions are set, show all data
    if (this.filterConditions.length === 0) {
      this.dataSource.data = this.occupations;
      this.closeFilterPanel();
      return;
    }

    // Apply filters to the data
    const filteredData = this.occupations.filter(occupation => {
      // Check each filter condition
      return this.filterConditions.every(condition => {
        // Handle IN operator (dropdown)
        if (condition.operator === 'IN' && condition.selectedValues) {
          // Filter out the __SELECT_ALL__ marker from selectedValues
          const actualSelectedValues = condition.selectedValues.filter((v: string) => v !== '__SELECT_ALL__');

          // If no values selected for this condition, skip it (treat as "all")
          if (actualSelectedValues.length === 0) {
            return true;
          }

          // Get the value from the occupation object
          const occupationValue = (occupation as any)[condition.field];

          // Check if the occupation value matches any of the selected filter values
          return actualSelectedValues.includes(occupationValue);
        }

        // Handle CONTAINS operator (text input)
        if (condition.operator === 'CONTAINS' && condition.textValue) {
          // If text is empty, skip this condition
          if (!condition.textValue.trim()) {
            return true;
          }

          // Get the value from the occupation object
          const occupationValue = (occupation as any)[condition.field];

          // Check if the occupation value contains the text (case-insensitive)
          return occupationValue?.toLowerCase().includes(condition.textValue.toLowerCase());
        }

        return true;
      });
    });

    // Update the table data
    this.dataSource.data = filteredData;
    this.closeFilterPanel();
  }
}

// Confirmation Delete Dialog Component for Occupation
@Component({
  selector: 'confirm-delete-occupation-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Xác nhận xóa</h2>
    <mat-dialog-content>
      <p>Bạn có chắc chắn muốn xóa nghề nghiệp này?</p>
      <p class="occupation-info"><strong>{{ data.occupationName }}</strong> ({{ data.occupationCode }})</p>
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
    .occupation-info {
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
export class ConfirmDeleteOccupationDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteOccupationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { occupationName: string; occupationCode: string }
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}

// No Selection Dialog Component
@Component({
  selector: 'no-selection-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Thông báo</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-raised-button color="primary" (click)="onClose()">Đóng</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      padding: 20px 24px;
      min-width: 300px;
    }
    mat-dialog-content p {
      margin: 0;
      font-size: 14px;
      color: #333;
    }
    mat-dialog-actions {
      padding: 16px 24px;
    }
  `]
})
export class NoSelectionDialog {
  constructor(
    public dialogRef: MatDialogRef<NoSelectionDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { message: string }
  ) {}

  onClose(): void {
    this.dialogRef.close();
  }
}
