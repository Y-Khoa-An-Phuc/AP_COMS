import { Occupation } from "./occupation";
import { Organization } from "./org";
import type { Branch } from "./branch";

export interface Employee {
  empId: string;
  email: string;
  fullName: string;
  lastName: string;
  middleName: string;
  firstName: string;
  hireDt: Date | string;
  terminationDt: Date | string | null;
  citizenIdCard: string;
  passport: string;
  contractId: string | null;
  occupation: Occupation;
  organization: Organization;
  branch: Branch;
  phone: string;
  supvEmployee: Employee | null;
  createUser: string;
  createTimestamp: Date | string;
  lastUpdateUser: string;
  lastUpdateTimestamp: Date | string;
  workStatus: string;
}
