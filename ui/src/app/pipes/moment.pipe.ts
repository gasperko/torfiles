import { Pipe, PipeTransform } from '@angular/core';
import * as moment from 'moment';

@Pipe({
  name: 'moment'
})
export class MomentPipe implements PipeTransform {

  transform(value: number): string {
      // return moment(value).format('MM-DD-YYYY');

      return (value!= null) ? moment(value).fromNow(true): '';
  }

}

