import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Auth } from './auth/auth';
import { Settings } from './settings/settings';
import { AddEmployee } from './employee/add/add';
import { ViewEmployee } from './employee/view/view';
import { ViewOccupation } from './occupation/view/view';
import { ChangePassword } from './change-password/change-password';
import { FirstLogin } from './first-login/first-login';

export const routes: Routes = [
    {
        path: 'home',
        component: Home
    },
    {
        path: 'settings',
        component: Settings,
        children: [
            { path: 'employee/view', component: ViewEmployee },
            { path: 'occupation/view', component: ViewOccupation }
        ]
    },
    { path: 'first-login', component: FirstLogin },
    { path: 'change-password', component: ChangePassword },
    { path: '', component: Auth }
];